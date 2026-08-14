package app.usenekko.home.data

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Note
import app.usenekko.home.domain.Reminder
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

data class ContactProfileSnapshot(
    val contact: Contact?,
    val notes: List<Note>,
    val reminders: List<Reminder>,
    val checkInCount: Int,
    val fetchedAt: Instant,
    val accountKey: String,
)

data class ContactProfileRepositoryState(
    val snapshots: Map<String, ContactProfileSnapshot> = emptyMap(),
    val refreshingContactIds: Set<String> = emptySet(),
    val errors: Map<String, ContactError> = emptyMap(),
)

interface ContactProfileRepository {
    val state: StateFlow<ContactProfileRepositoryState>

    suspend fun load(
        contactId: String,
        forceRefresh: Boolean = false,
    ): Result<ContactProfileSnapshot, ContactError>

    fun invalidate(contactId: String)
}

class InMemoryContactProfileRepository(
    private val contactDataSource: ContactDataSource,
    private val accountKeyProvider: suspend () -> String?,
    private val scope: CoroutineScope,
    private val now: () -> Instant = { Clock.System.now() },
    private val staleAfter: Duration = 30.toDuration(DurationUnit.SECONDS),
) : ContactProfileRepository {
    private val _state = MutableStateFlow(ContactProfileRepositoryState())
    override val state: StateFlow<ContactProfileRepositoryState> = _state.asStateFlow()

    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, InFlight>()
    private var accountKey: String? = null
    private val invalidatedContactIds = mutableSetOf<String>()

    override suspend fun load(
        contactId: String,
        forceRefresh: Boolean,
    ): Result<ContactProfileSnapshot, ContactError> {
        val currentAccountKey = accountKeyProvider()
        if (currentAccountKey == null) {
            clearCache()
            return Result.Error(ContactError.NotAuthenticated)
        }

        val cached = mutex.withLock {
            if (accountKey != null && accountKey != currentAccountKey) {
                inFlight.values.forEach { it.deferred.cancel() }
                inFlight.clear()
                invalidatedContactIds.clear()
                _state.value = ContactProfileRepositoryState()
            }
            accountKey = currentAccountKey
            _state.value.snapshots[contactId]
        }
        val invalidated = mutex.withLock { contactId in invalidatedContactIds }

        if (
            cached != null &&
            !forceRefresh &&
            !invalidated &&
            now() - cached.fetchedAt < staleAfter
        ) {
            return Result.Success(cached)
        }

        if (cached != null && !forceRefresh) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                refresh(contactId, currentAccountKey)
            }
            return Result.Success(cached)
        }

        return refresh(contactId, currentAccountKey)
    }

    override fun invalidate(contactId: String) {
        invalidatedContactIds += contactId
        _state.value = _state.value.copy(errors = _state.value.errors - contactId)
    }

    private suspend fun clearCache() {
        mutex.withLock {
            inFlight.values.forEach { it.deferred.cancel() }
            inFlight.clear()
            invalidatedContactIds.clear()
            accountKey = null
            _state.value = ContactProfileRepositoryState()
        }
    }

    private suspend fun refresh(
        contactId: String,
        currentAccountKey: String,
    ): Result<ContactProfileSnapshot, ContactError> {
        var startNew = false
        val deferred = mutex.withLock {
            val current = inFlight[contactId]
            if (
                current != null &&
                current.accountKey == currentAccountKey &&
                current.deferred.isActive
            ) {
                current.deferred
            } else {
                current?.deferred?.cancel()
                lateinit var created: Deferred<Result<ContactProfileSnapshot, ContactError>>
                created = scope.async(start = CoroutineStart.LAZY) {
                    try {
                        fetchAndPublish(contactId, currentAccountKey)
                    } finally {
                        mutex.withLock {
                            if (inFlight[contactId]?.deferred === created) {
                                inFlight.remove(contactId)
                            }
                        }
                    }
                }
                inFlight[contactId] = InFlight(currentAccountKey, created)
                invalidatedContactIds.remove(contactId)
                _state.value = _state.value.copy(
                    refreshingContactIds = _state.value.refreshingContactIds + contactId,
                    errors = _state.value.errors - contactId,
                )
                startNew = true
                created
            }
        }
        if (startNew) deferred.start()
        return deferred.await()
    }

    private suspend fun fetchAndPublish(
        contactId: String,
        expectedAccountKey: String,
    ): Result<ContactProfileSnapshot, ContactError> {
        val contactsResult = contactDataSource.getContacts()
        if (contactsResult is Result.Error) return finishWithError(contactId, contactsResult.error)

        val notesResult = contactDataSource.getNotes(contactId)
        if (notesResult is Result.Error) return finishWithError(contactId, notesResult.error)

        val remindersResult = contactDataSource.getReminders(contactId)
        if (remindersResult is Result.Error) return finishWithError(contactId, remindersResult.error)

        val checkInsResult = contactDataSource.getCheckIns(contactId, "1970-01-01", "2999-12-31")
        if (checkInsResult is Result.Error) return finishWithError(contactId, checkInsResult.error)

        if (accountKeyProvider() != expectedAccountKey) {
            return finishWithError(contactId, ContactError.NotAuthenticated)
        }

        val snapshot = ContactProfileSnapshot(
            contact = (contactsResult as Result.Success).data.firstOrNull { it.id == contactId },
            notes = (notesResult as Result.Success).data,
            reminders = (remindersResult as Result.Success).data,
            checkInCount = (checkInsResult as Result.Success).data.size,
            fetchedAt = now(),
            accountKey = expectedAccountKey,
        )
        _state.value = _state.value.copy(
            snapshots = _state.value.snapshots + (contactId to snapshot),
            refreshingContactIds = _state.value.refreshingContactIds - contactId,
            errors = _state.value.errors - contactId,
        )
        return Result.Success(snapshot)
    }

    private fun finishWithError(contactId: String, error: ContactError): Result<ContactProfileSnapshot, ContactError> {
        _state.value = _state.value.copy(
            refreshingContactIds = _state.value.refreshingContactIds - contactId,
            errors = _state.value.errors + (contactId to error),
        )
        return Result.Error(error)
    }

    private data class InFlight(
        val accountKey: String,
        val deferred: Deferred<Result<ContactProfileSnapshot, ContactError>>,
    )
}