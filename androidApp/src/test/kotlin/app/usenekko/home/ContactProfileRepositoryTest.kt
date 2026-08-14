package app.usenekko.home

import app.usenekko.home.data.InMemoryContactProfileRepository
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.domain.Note
import app.usenekko.home.domain.Reminder
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ContactProfileRepositoryTest {
    private val contact = Contact(
        id = "c1",
        name = "Liam",
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = "07:30:00",
        nextCheckInDate = "2030-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )
    private val note = Note("n1", "c1", "Birthday", "Send a card", "2026-08-14T10:00:00Z")
    private val reminder = Reminder("r1", "c1", "Call", "Say hello", "weekly", null)
    private val checkIn = CheckIn("i1", "c1", "2026-08-14T10:00:00Z", null)

    @Test
    fun coldLoadFetchesAndPublishesCompleteSnapshot() = runTest {
        val dataSource = CountingProfileDataSource(contact, note, reminder, checkIn)
        val repository = repository(dataSource, this)

        val result = repository.load("c1")

        val snapshot = (result as Result.Success).data
        assertEquals("Liam", snapshot.contact?.name)
        assertEquals(listOf(note), snapshot.notes)
        assertEquals(listOf(reminder), snapshot.reminders)
        assertEquals(1, snapshot.checkInCount)
        assertEquals(4, dataSource.totalReads)
    }

    @Test
    fun warmLoadReturnsCachedProfileWithoutReads() = runTest {
        val dataSource = CountingProfileDataSource(contact, note, reminder, checkIn)
        val repository = repository(dataSource, this)
        repository.load("c1")
        dataSource.resetCounts()

        val result = repository.load("c1")

        assertEquals("Liam", (result as Result.Success).data.contact?.name)
        assertEquals(0, dataSource.totalReads)
    }

    @Test
    fun staleLoadReturnsCacheThenPublishesRefresh() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        val dataSource = CountingProfileDataSource(contact, note, reminder, checkIn)
        val repository = repository(dataSource, this, now = { now })
        repository.load("c1")
        dataSource.contactsResult = Result.Success(listOf(contact.copy(name = "Updated Liam")))
        dataSource.resetCounts()
        now = Instant.parse("2026-08-14T12:00:31Z")

        val result = repository.load("c1")
        assertEquals("Liam", (result as Result.Success).data.contact?.name)
        advanceUntilIdle()

        assertEquals("Updated Liam", repository.state.value.snapshots["c1"]?.contact?.name)
        assertEquals(4, dataSource.totalReads)
    }

    @Test
    fun failedRefreshRetainsCachedProfileAndReportsError() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        val dataSource = CountingProfileDataSource(contact, note, reminder, checkIn)
        val repository = repository(dataSource, this, now = { now })
        repository.load("c1")
        dataSource.contactsResult = Result.Error(ContactError.Network)
        now = Instant.parse("2026-08-14T12:00:31Z")

        assertTrue(repository.load("c1") is Result.Success)
        advanceUntilIdle()

        assertEquals("Liam", repository.state.value.snapshots["c1"]?.contact?.name)
        assertEquals(ContactError.Network, repository.state.value.errors["c1"])
    }

    @Test
    fun concurrentLoadsShareOneRefreshBatch() = runTest {
        val dataSource = CountingProfileDataSource(contact, note, reminder, checkIn)
        val repository = repository(dataSource, this)
        repository.load("c1")
        dataSource.resetCounts()
        repository.invalidate("c1")

        val first = async { repository.load("c1") }
        val second = async { repository.load("c1") }
        advanceUntilIdle()

        assertTrue(first.await() is Result.Success)
        assertTrue(second.await() is Result.Success)
        assertEquals(4, dataSource.totalReads)
    }

    @Test
    fun changingAccountNeverReturnsPreviousProfile() = runTest {
        var account = "account-a"
        val dataSource = CountingProfileDataSource(contact, note, reminder, checkIn)
        val repository = repository(dataSource, this, account = { account })
        repository.load("c1")
        dataSource.contactsResult = Result.Success(listOf(contact.copy(name = "Account B")))
        account = "account-b"

        val result = repository.load("c1")

        assertEquals("Account B", (result as Result.Success).data.contact?.name)
        assertEquals("account-b", repository.state.value.snapshots["c1"]?.accountKey)
    }

    private fun repository(
        dataSource: CountingProfileDataSource,
        scope: CoroutineScope,
        account: suspend () -> String? = { "account-a" },
        now: () -> Instant = { Instant.parse("2026-08-14T12:00:00Z") },
    ) = InMemoryContactProfileRepository(
        contactDataSource = dataSource,
        accountKeyProvider = account,
        scope = scope,
        now = now,
    )
}

private class CountingProfileDataSource(
    contact: Contact,
    note: Note,
    reminder: Reminder,
    checkIn: CheckIn,
) : ContactDataSource by FakeContactDataSource(
    contacts = listOf(contact),
    notes = listOf(note),
    reminders = listOf(reminder),
    checkIns = listOf(checkIn),
    groups = emptyList<Group>(),
    memberships = emptyList<GroupMembership>(),
) {
    var contactsResult: Result<List<Contact>, ContactError> = Result.Success(listOf(contact))
    var notesResult: Result<List<Note>, ContactError> = Result.Success(listOf(note))
    var remindersResult: Result<List<Reminder>, ContactError> = Result.Success(listOf(reminder))
    var checkInsResult: Result<List<CheckIn>, ContactError> = Result.Success(listOf(checkIn))

    var contactsReads = 0
    var notesReads = 0
    var remindersReads = 0
    var checkInsReads = 0

    val totalReads: Int
        get() = contactsReads + notesReads + remindersReads + checkInsReads

    override suspend fun getContacts(): Result<List<Contact>, ContactError> {
        contactsReads++
        return contactsResult
    }

    override suspend fun getNotes(contactId: String): Result<List<Note>, ContactError> {
        notesReads++
        return notesResult
    }

    override suspend fun getReminders(contactId: String): Result<List<Reminder>, ContactError> {
        remindersReads++
        return remindersResult
    }

    override suspend fun getCheckIns(
        contactId: String?,
        from: String,
        to: String,
    ): Result<List<CheckIn>, ContactError> {
        checkInsReads++
        return checkInsResult
    }

    fun resetCounts() {
        contactsReads = 0
        notesReads = 0
        remindersReads = 0
        checkInsReads = 0
    }
}