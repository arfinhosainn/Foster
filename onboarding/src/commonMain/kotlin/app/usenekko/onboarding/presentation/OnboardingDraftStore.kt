package app.usenekko.onboarding.presentation

import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class OnboardingDraftStore(
    private val localDataSource: OnboardingDraftLocalDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    init {
        scope.launch {
            _draft.value = localDataSource.getDraft()
        }
    }

    fun update(transform: (OnboardingDraft) -> OnboardingDraft) {
        val nextDraft = transform(_draft.value)
            .copy(lastUpdatedAtMillis = Clock.System.now().toEpochMilliseconds())
        _draft.value = nextDraft
        scope.launch {
            localDataSource.saveDraft(nextDraft)
        }
    }

    fun clear() {
        _draft.value = OnboardingDraft()
        scope.launch {
            localDataSource.clearDraft()
        }
    }
}
