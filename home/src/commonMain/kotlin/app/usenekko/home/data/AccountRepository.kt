package app.usenekko.home.data

import app.usenekko.home.domain.BadgeSlot
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.toUserMessage
import app.usenekko.shared.domain.AccountProfile
import app.usenekko.shared.domain.ProfileDataSource
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

data class AccountSnapshot(
    val profile: AccountProfile?,
    val badgeSlots: List<BadgeSlot>,
    val fetchedAt: Instant,
    val accountKey: String,
)

data class AccountRepositoryState(
    val snapshot: AccountSnapshot? = null,
    val isRefreshing: Boolean = false,
    val isUpdatingAvatar: Boolean = false,
    val error: String? = null,
)

interface AccountRepository {
    val state: StateFlow<AccountRepositoryState>

    suspend fun load(forceRefresh: Boolean = false): Result<AccountSnapshot, String>

    suspend fun updateSelectedAvatarId(selectedAvatarId: String): Result<Unit, String>

    fun invalidate()
}

class InMemoryAccountRepository(
    private val profileDataSource: ProfileDataSource,
    private val contactDataSource: ContactDataSource,
    private val accountKeyProvider: suspend () -> String?,
    private val scope: CoroutineScope,
    private val now: () -> Instant = { Clock.System.now() },
    private val staleAfter: Duration = 30.toDuration(DurationUnit.SECONDS),
) : AccountRepository {
    private val _state = MutableStateFlow(AccountRepositoryState())
    override val state: StateFlow<AccountRepositoryState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var invalidated = false
    private var inFlight: InFlight? = null
    private var cachedAccountKey: String? = null

    override suspend fun load(forceRefresh: Boolean): Result<AccountSnapshot, String> {
        val accountKey = accountKeyProvider()
        if (accountKey == null) {
            clearCache()
            return Result.Error("Not authenticated")
        }

        val cached = mutex.withLock {
            if (cachedAccountKey != null && cachedAccountKey != accountKey) {
                inFlight?.deferred?.cancel()
                inFlight = null
                invalidated = false
                _state.value = AccountRepositoryState()
            }
            cachedAccountKey = accountKey
            _state.value.snapshot
        }
        val cacheIsInvalidated = mutex.withLock { invalidated }

        if (
            cached != null &&
            !forceRefresh &&
            !cacheIsInvalidated &&
            now() - cached.fetchedAt < staleAfter
        ) {
            return Result.Success(cached)
        }

        if (cached != null && !forceRefresh) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                refresh(accountKey)
            }
            return Result.Success(cached)
        }

        return refresh(accountKey)
    }

    override fun invalidate() {
        invalidated = true
        _state.value = _state.value.copy(error = null)
    }

    override suspend fun updateSelectedAvatarId(selectedAvatarId: String): Result<Unit, String> {
        _state.value = _state.value.copy(isUpdatingAvatar = true, error = null)

        return when (val result = profileDataSource.updateSelectedAvatarId(selectedAvatarId)) {
            is Result.Success -> when (val refreshResult = load(forceRefresh = true)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(isUpdatingAvatar = false)
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(isUpdatingAvatar = false)
                    Result.Error(refreshResult.error)
                }
            }
            is Result.Error -> {
                val error = result.error.toUserMessage()
                _state.value = _state.value.copy(isUpdatingAvatar = false, error = error)
                Result.Error(error)
            }
        }
    }

    private suspend fun clearCache() {
        mutex.withLock {
            inFlight?.deferred?.cancel()
            inFlight = null
            invalidated = false
            cachedAccountKey = null
            _state.value = AccountRepositoryState()
        }
    }

    private suspend fun refresh(accountKey: String): Result<AccountSnapshot, String> {
        var startNew = false
        val deferred = mutex.withLock {
            val current = inFlight
            if (current != null && current.accountKey == accountKey && current.deferred.isActive) {
                current.deferred
            } else {
                current?.deferred?.cancel()
                lateinit var created: Deferred<Result<AccountSnapshot, String>>
                created = scope.async(start = CoroutineStart.LAZY) {
                    try {
                        fetchAndPublish(accountKey)
                    } finally {
                        mutex.withLock {
                            if (inFlight?.deferred === created) inFlight = null
                        }
                    }
                }
                inFlight = InFlight(accountKey, created)
                invalidated = false
                _state.value = _state.value.copy(isRefreshing = true, error = null)
                startNew = true
                created
            }
        }
        if (startNew) deferred.start()
        return deferred.await()
    }

    private suspend fun fetchAndPublish(accountKey: String): Result<AccountSnapshot, String> {
        val profileDeferred = scope.async { profileDataSource.getProfile() }
        val badgesDeferred = scope.async { contactDataSource.getBadges() }
        val userBadgesDeferred = scope.async { contactDataSource.getUserBadges() }

        val profileResult = profileDeferred.await()
        val badgesResult = badgesDeferred.await()
        val userBadgesResult = userBadgesDeferred.await()
        val badgeError = listOfNotNull(
            (badgesResult as? Result.Error)?.error?.toUserMessage(),
            (userBadgesResult as? Result.Error)?.error?.toUserMessage(),
        ).firstOrNull()

        if (badgeError != null) {
            return finishWithError(badgeError)
        }

        if (accountKeyProvider() != accountKey) {
            return finishWithError("Not authenticated")
        }

        val catalog = (badgesResult as Result.Success).data
        val unlockedIds = (userBadgesResult as Result.Success).data
            .mapTo(mutableSetOf()) { it.badgeId }
        val snapshot = AccountSnapshot(
            profile = (profileResult as? Result.Success)?.data,
            badgeSlots = catalog
                .sortedBy { it.threshold }
                .map { badge -> BadgeSlot(badge, badge.id in unlockedIds) },
            fetchedAt = now(),
            accountKey = accountKey,
        )
        _state.value = AccountRepositoryState(
            snapshot = snapshot,
            isUpdatingAvatar = _state.value.isUpdatingAvatar,
        )
        return Result.Success(snapshot)
    }

    private fun finishWithError(error: String): Result<AccountSnapshot, String> {
        _state.value = _state.value.copy(isRefreshing = false, error = error)
        return Result.Error(error)
    }

    private data class InFlight(
        val accountKey: String,
        val deferred: Deferred<Result<AccountSnapshot, String>>,
    )
}