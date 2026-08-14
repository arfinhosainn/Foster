package app.usenekko.home.data

import app.usenekko.home.domain.BrainstormDataSource
import app.usenekko.home.domain.BrainstormError
import app.usenekko.home.domain.BrainstormGeneration
import app.usenekko.home.domain.BrainstormSession
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

data class BrainstormHistorySnapshot(
    val contactId: String,
    val history: List<BrainstormSession>,
    val fetchedAt: Instant,
    val accountKey: String,
)

data class BrainstormRepositoryState(
    val snapshot: BrainstormHistorySnapshot? = null,
    val isRefreshing: Boolean = false,
    val error: BrainstormError? = null,
)

interface BrainstormRepository {
    fun state(contactId: String): StateFlow<BrainstormRepositoryState>

    suspend fun load(
        contactId: String,
        forceRefresh: Boolean = false,
    ): Result<BrainstormHistorySnapshot, BrainstormError>

    suspend fun generate(contactId: String): Result<BrainstormGeneration, BrainstormError>

    fun invalidate(contactId: String)
}

class InMemoryBrainstormRepository(
    private val dataSource: BrainstormDataSource,
    private val accountKeyProvider: () -> String?,
    private val scope: CoroutineScope,
    private val now: () -> Instant = { Clock.System.now() },
    private val staleAfter: Duration = 30.toDuration(DurationUnit.SECONDS),
) : BrainstormRepository {
    private val states = mutableMapOf<String, MutableStateFlow<BrainstormRepositoryState>>()
    private val mutex = Mutex()
    private val invalidatedContacts = mutableSetOf<String>()
    private val inFlight = mutableMapOf<RefreshKey, InFlight>()
    private var activeAccountKey: String? = null

    override fun state(contactId: String): StateFlow<BrainstormRepositoryState> =
        stateFlow(contactId).asStateFlow()

    override suspend fun load(
        contactId: String,
        forceRefresh: Boolean,
    ): Result<BrainstormHistorySnapshot, BrainstormError> {
        val accountKey = accountKeyProvider()
            ?: return clearForUnauthenticatedUser()
        val state = stateFlow(contactId)
        val cached = mutex.withLock {
            if (activeAccountKey != null && activeAccountKey != accountKey) {
                clearAccountLocked(accountKey)
            } else {
                activeAccountKey = accountKey
            }
            state.value.snapshot?.takeIf { it.accountKey == accountKey }
        }
        val isInvalidated = mutex.withLock { contactId in invalidatedContacts }

        if (
            cached != null &&
            !forceRefresh &&
            !isInvalidated &&
            now() - cached.fetchedAt < staleAfter
        ) {
            return Result.Success(cached)
        }

        if (cached != null && !forceRefresh) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                refresh(accountKey, contactId, state)
            }
            return Result.Success(cached)
        }

        return refresh(accountKey, contactId, state)
    }

    override fun invalidate(contactId: String) {
        invalidatedContacts += contactId
        states[contactId]?.let { state ->
            state.value = state.value.copy(error = null)
        }
    }

    override suspend fun generate(
        contactId: String,
    ): Result<BrainstormGeneration, BrainstormError> {
        return when (val result = dataSource.generate(contactId)) {
            is Result.Success -> {
                if (result.data is BrainstormGeneration.Generated) invalidate(contactId)
                result
            }
            is Result.Error -> result
        }
    }

    private fun stateFlow(contactId: String): MutableStateFlow<BrainstormRepositoryState> =
        states.getOrPut(contactId) { MutableStateFlow(BrainstormRepositoryState()) }

    private suspend fun clearForUnauthenticatedUser(): Result<BrainstormHistorySnapshot, BrainstormError> {
        mutex.withLock {
            clearAccountLocked(null)
        }
        return Result.Error(BrainstormError.NotAuthenticated)
    }

    private fun clearAccountLocked(accountKey: String?) {
        inFlight.values.forEach { it.deferred.cancel() }
        inFlight.clear()
        invalidatedContacts.clear()
        states.values.forEach { it.value = BrainstormRepositoryState() }
        activeAccountKey = accountKey
    }

    private suspend fun refresh(
        accountKey: String,
        contactId: String,
        state: MutableStateFlow<BrainstormRepositoryState>,
    ): Result<BrainstormHistorySnapshot, BrainstormError> {
        var startNew = false
        val refreshKey = RefreshKey(accountKey, contactId)
        val deferred = mutex.withLock {
            val current = inFlight[refreshKey]
            if (current != null && current.deferred.isActive) {
                current.deferred
            } else {
                lateinit var created: Deferred<Result<BrainstormHistorySnapshot, BrainstormError>>
                created = scope.async(start = CoroutineStart.LAZY) {
                    try {
                        fetchAndPublish(accountKey, contactId, state)
                    } finally {
                        mutex.withLock {
                            if (inFlight[refreshKey]?.deferred === created) inFlight.remove(refreshKey)
                        }
                    }
                }
                inFlight[refreshKey] = InFlight(created)
                invalidatedContacts -= contactId
                state.value = state.value.copy(isRefreshing = true, error = null)
                startNew = true
                created
            }
        }
        if (startNew) deferred.start()
        return deferred.await()
    }

    private suspend fun fetchAndPublish(
        accountKey: String,
        contactId: String,
        state: MutableStateFlow<BrainstormRepositoryState>,
    ): Result<BrainstormHistorySnapshot, BrainstormError> {
        return when (val result = dataSource.getHistory(contactId)) {
            is Result.Success -> {
                if (accountKeyProvider() != accountKey) {
                    finishWithError(state, BrainstormError.NotAuthenticated)
                } else {
                    val snapshot = BrainstormHistorySnapshot(
                        contactId = contactId,
                        history = result.data,
                        fetchedAt = now(),
                        accountKey = accountKey,
                    )
                    state.value = BrainstormRepositoryState(snapshot = snapshot)
                    Result.Success(snapshot)
                }
            }
            is Result.Error -> finishWithError(state, result.error)
        }
    }

    private fun finishWithError(
        state: MutableStateFlow<BrainstormRepositoryState>,
        error: BrainstormError,
    ): Result<BrainstormHistorySnapshot, BrainstormError> {
        state.value = state.value.copy(isRefreshing = false, error = error)
        return Result.Error(error)
    }

    private data class RefreshKey(
        val accountKey: String,
        val contactId: String,
    )

    private data class InFlight(
        val deferred: Deferred<Result<BrainstormHistorySnapshot, BrainstormError>>,
    )
}