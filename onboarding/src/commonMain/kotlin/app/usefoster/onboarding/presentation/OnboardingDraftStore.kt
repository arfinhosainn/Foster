package app.usefoster.onboarding.presentation

import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlin.time.Clock

class OnboardingDraftStore(
    private val localDataSource: OnboardingDraftLocalDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistenceMutex = Mutex()
    private val _storageErrors = Channel<OnboardingDraftStorageError>(Channel.BUFFERED)
    private val latestPersistenceRequest = MutableStateFlow(0L)

    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()
    val storageErrors: Flow<OnboardingDraftStorageError> = _storageErrors.receiveAsFlow()

    init {
        scope.launch {
            when (val result = localDataSource.getDraft()) {
                is Result.Success -> {
                    if (latestPersistenceRequest.value == 0L) _draft.value = result.data
                }
                is Result.Error -> {
                    if (latestPersistenceRequest.value == 0L) reportStorageError(result.error)
                }
            }
        }
    }

    fun update(transform: (OnboardingDraft) -> OnboardingDraft) {
        val requestId = nextPersistenceRequest()
        val nextDraft = transform(_draft.value)
            .copy(lastUpdatedAtMillis = Clock.System.now().toEpochMilliseconds())
        _draft.value = nextDraft
        scope.launch {
            persistenceMutex.withLock {
                if (requestId != latestPersistenceRequest.value) return@withLock
                when (val result = localDataSource.saveDraft(nextDraft)) {
                    is Result.Success -> Unit
                    is Result.Error -> {
                        if (requestId == latestPersistenceRequest.value) reportStorageError(result.error)
                    }
                }
            }
        }
    }

    fun clear() {
        val requestId = nextPersistenceRequest()
        val draftBeforeClear = _draft.value
        _draft.value = OnboardingDraft()
        scope.launch {
            persistenceMutex.withLock {
                if (requestId != latestPersistenceRequest.value) return@withLock
                when (val result = localDataSource.clearDraft()) {
                    is Result.Success -> Unit
                    is Result.Error -> {
                        if (requestId == latestPersistenceRequest.value && _draft.value == OnboardingDraft()) {
                            _draft.value = draftBeforeClear
                        }
                        if (requestId == latestPersistenceRequest.value) reportStorageError(result.error)
                    }
                }
            }
        }
    }

    private fun nextPersistenceRequest(): Long {
        var requestId = 0L
        latestPersistenceRequest.update {
            requestId = it + 1
            requestId
        }
        return requestId
    }

    private fun reportStorageError(error: OnboardingDraftStorageError) {
        _storageErrors.trySend(error)
    }
}
