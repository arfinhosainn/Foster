package app.usefoster.home.data

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactDataSource
import app.usefoster.home.domain.ContactError
import app.usefoster.home.domain.Group
import app.usefoster.home.domain.GroupMembership
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.domain.Reminder
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

data class HomeSnapshot(
    val contacts: List<Contact>,
    val groups: List<Group>,
    val memberships: List<GroupMembership>,
    val recentCheckIns: List<CheckIn>,
    val checkInHistory: List<CheckIn>,
    val missedCheckIns: List<MissedCheckIn>,
    val customReminders: List<Reminder> = emptyList(),
    val fetchedAt: Instant,
    val accountKey: String,
    val localDate: LocalDate,
)

data class HomeRepositoryState(
    val snapshot: HomeSnapshot? = null,
    val isRefreshing: Boolean = false,
    val error: ContactError? = null,
)

data class HomeGroupPickerState(
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val memberships: List<GroupMembership> = emptyList(),
    val isLoading: Boolean = true,
    val error: ContactError? = null,
)

interface HomeRepository {
    val state: StateFlow<HomeRepositoryState>
    val groupPickerState: Flow<HomeGroupPickerState>

    suspend fun load(forceRefresh: Boolean = false): Result<HomeSnapshot, ContactError>

    fun invalidate()
}

class InMemoryHomeRepository(
    private val contactDataSource: ContactDataSource,
    private val accountKeyProvider: suspend () -> String?,
    private val scope: CoroutineScope,
    private val now: () -> Instant = { Clock.System.now() },
    private val today: () -> LocalDate = {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    },
    private val staleAfter: Duration = 30.toDuration(DurationUnit.SECONDS),
) : HomeRepository {
    private val _state = MutableStateFlow(HomeRepositoryState())
    override val state: StateFlow<HomeRepositoryState> = _state.asStateFlow()
    override val groupPickerState: Flow<HomeGroupPickerState> = state
        .map { repositoryState ->
            val snapshot = repositoryState.snapshot
            HomeGroupPickerState(
                groups = snapshot?.groups.orEmpty(),
                contacts = snapshot?.contacts.orEmpty(),
                memberships = snapshot?.memberships.orEmpty(),
                isLoading = snapshot == null && repositoryState.error == null,
                error = repositoryState.error,
            )
        }
        .distinctUntilChanged()

    private val mutex = Mutex()
    private var invalidated = false
    private var inFlight: InFlight? = null

    override suspend fun load(forceRefresh: Boolean): Result<HomeSnapshot, ContactError> {
        val accountKey = accountKeyProvider()
        if (accountKey == null) {
            clearCache()
            return Result.Error(ContactError.NotAuthenticated)
        }

        val cached = mutex.withLock {
            val snapshot = _state.value.snapshot
            if (snapshot != null && snapshot.accountKey != accountKey) {
                inFlight?.deferred?.cancel()
                inFlight = null
                invalidated = false
                _state.value = HomeRepositoryState()
                null
            } else {
                snapshot
            }
        }
        val cacheIsInvalidated = mutex.withLock { invalidated }

        if (
            cached != null &&
            !forceRefresh &&
            !cacheIsInvalidated &&
            !cached.isStale(now(), today(), staleAfter)
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

    private suspend fun clearCache() {
        mutex.withLock {
            inFlight?.deferred?.cancel()
            inFlight = null
            invalidated = false
            _state.value = HomeRepositoryState()
        }
    }

    private suspend fun refresh(accountKey: String): Result<HomeSnapshot, ContactError> {
        var startNew = false
        val deferred = mutex.withLock {
            val current = inFlight
            if (current != null && current.accountKey == accountKey && current.deferred.isActive) {
                current.deferred
            } else {
                current?.deferred?.cancel()
                lateinit var created: Deferred<Result<HomeSnapshot, ContactError>>
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

    private suspend fun fetchAndPublish(accountKey: String): Result<HomeSnapshot, ContactError> {
        val today = today()

        // All fetches are independent — run them in parallel. Previously these
        // were sequential, so a refresh cost the SUM of 7 network round trips,
        // which made surfaces like the add-contact group step crawl whenever
        // the cache was cold or invalidated.
        val (
            contactsResult,
            groupsResult,
            membershipsResult,
            recentCheckInsResult,
            checkInHistoryResult,
            missedCheckInsResult,
            customRemindersResult,
        ) = coroutineScope {
            val contacts = async { contactDataSource.getContacts() }
            val groups = async { contactDataSource.getGroups() }
            val memberships = async { contactDataSource.getGroupMemberships() }
            val recentCheckIns = async {
                contactDataSource.getCheckIns(
                    contactId = null,
                    from = today.minus(DatePeriod(days = 12)).toString(),
                    to = today.plus(DatePeriod(days = 13)).toString(),
                )
            }
            val checkInHistory = async {
                contactDataSource.getCheckIns(
                    contactId = null,
                    from = "1970-01-01",
                    to = "2999-12-31",
                )
            }
            val missedCheckIns = async {
                contactDataSource.getMissedCheckIns(
                    from = "1970-01-01",
                    to = today.toString(),
                )
            }
            val customReminders = async { contactDataSource.getAllCustomReminders() }

            // Awaiting all keeps error-checking in the original priority order.
            FetchResults(
                contacts = contacts.await(),
                groups = groups.await(),
                memberships = memberships.await(),
                recentCheckIns = recentCheckIns.await(),
                checkInHistory = checkInHistory.await(),
                missedCheckIns = missedCheckIns.await(),
                customReminders = customReminders.await(),
            )
        }

        if (contactsResult is Result.Error) return finishWithError(contactsResult.error)
        if (groupsResult is Result.Error) return finishWithError(groupsResult.error)
        if (membershipsResult is Result.Error) return finishWithError(membershipsResult.error)
        if (recentCheckInsResult is Result.Error) return finishWithError(recentCheckInsResult.error)
        if (checkInHistoryResult is Result.Error) return finishWithError(checkInHistoryResult.error)
        if (missedCheckInsResult is Result.Error) return finishWithError(missedCheckInsResult.error)
        if (customRemindersResult is Result.Error) return finishWithError(customRemindersResult.error)

        if (accountKeyProvider() != accountKey) {
            return finishWithError(ContactError.NotAuthenticated)
        }

        val snapshot = HomeSnapshot(
            contacts = (contactsResult as Result.Success).data,
            groups = (groupsResult as Result.Success).data,
            memberships = (membershipsResult as Result.Success).data,
            recentCheckIns = (recentCheckInsResult as Result.Success).data,
            checkInHistory = (checkInHistoryResult as Result.Success).data,
            missedCheckIns = (missedCheckInsResult as Result.Success).data,
            customReminders = (customRemindersResult as Result.Success).data,
            fetchedAt = now(),
            accountKey = accountKey,
            localDate = today,
        )
        _state.value = HomeRepositoryState(snapshot = snapshot)
        return Result.Success(snapshot)
    }

    /** Bundles the parallel fetch outcomes so they can be destructured cleanly. */
    private data class FetchResults(
        val contacts: Result<List<Contact>, ContactError>,
        val groups: Result<List<Group>, ContactError>,
        val memberships: Result<List<GroupMembership>, ContactError>,
        val recentCheckIns: Result<List<CheckIn>, ContactError>,
        val checkInHistory: Result<List<CheckIn>, ContactError>,
        val missedCheckIns: Result<List<MissedCheckIn>, ContactError>,
        val customReminders: Result<List<Reminder>, ContactError>,
    )

    private fun finishWithError(error: ContactError): Result<HomeSnapshot, ContactError> {
        _state.value = _state.value.copy(isRefreshing = false, error = error)
        return Result.Error(error)
    }

    private fun HomeSnapshot.isStale(
        currentTime: Instant,
        currentDate: LocalDate,
        staleAfter: Duration,
    ): Boolean = localDate != currentDate || currentTime - fetchedAt >= staleAfter

    private data class InFlight(
        val accountKey: String,
        val deferred: Deferred<Result<HomeSnapshot, ContactError>>,
    )
}