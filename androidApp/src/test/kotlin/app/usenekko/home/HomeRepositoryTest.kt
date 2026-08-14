package app.usenekko.home

import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryTest {
    private val contact = Contact(
        id = "c1",
        name = "Arfin",
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = "12:00:00",
        nextCheckInDate = "2099-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )
    private val group = Group(id = "g1", name = "Family")
    private val membership = GroupMembership(contactId = "c1", groupId = "g1")
    private val checkIn = CheckIn(
        id = "i1",
        contactId = "c1",
        checkedInAt = "2026-08-14T12:00:00Z",
    )

    @Test
    fun coldLoadFetchesAndPublishesOneCompleteSnapshot() = runTest {
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this)

        val result = repository.load()

        assertEquals(Result.Success(contact), result.mapSnapshot { it.contacts.single() })
        assertEquals(5, dataSource.totalHomeReads)
        assertEquals(contact, repository.state.value.snapshot?.contacts?.single())
        assertFalse(repository.state.value.isRefreshing)
    }

    @Test
    fun warmLoadReturnsCacheWithoutIssuingReads() = runTest {
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this)
        repository.load()
        dataSource.resetCounts()

        val result = repository.load()

        assertTrue(result is Result.Success)
        assertEquals(0, dataSource.totalHomeReads)
    }

    @Test
    fun staleLoadReturnsCacheAndRefreshesInBackground() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this, now = { now })
        repository.load()
        dataSource.contactsResult = Result.Success(listOf(contact.copy(name = "Updated")))
        dataSource.resetCounts()
        now = Instant.parse("2026-08-14T12:00:31Z")

        val result = repository.load()
        assertEquals("Arfin", (result as Result.Success).data.contacts.single().name)
        assertTrue(repository.state.value.isRefreshing)

        advanceUntilIdle()

        assertEquals("Updated", repository.state.value.snapshot?.contacts?.single()?.name)
        assertEquals(5, dataSource.totalHomeReads)
        assertFalse(repository.state.value.isRefreshing)
    }

    @Test
    fun concurrentRefreshesShareOneRequestBatch() = runTest {
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this)
        repository.load()
        dataSource.resetCounts()
        repository.invalidate()

        val first = async { repository.load() }
        val second = async { repository.load() }
        advanceUntilIdle()

        assertTrue(first.await() is Result.Success)
        assertTrue(second.await() is Result.Success)
        assertEquals(5, dataSource.totalHomeReads)
    }

    @Test
    fun invalidatedSnapshotRefreshesWithFreshBatch() = runTest {
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this)
        repository.load()
        dataSource.contactsResult = Result.Success(listOf(contact.copy(name = "Updated")))
        dataSource.resetCounts()

        repository.invalidate()
        val result = repository.load(forceRefresh = true)

        assertEquals("Updated", (result as Result.Success).data.contacts.single().name)
        assertEquals(5, dataSource.totalHomeReads)
        assertFalse(repository.state.value.isRefreshing)
    }

    @Test
    fun failedRefreshRetainsUsableSnapshotAndReportsError() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this, now = { now })
        repository.load()
        dataSource.contactsResult = Result.Error(ContactError.Network)
        now = Instant.parse("2026-08-14T12:00:31Z")

        val result = repository.load()
        advanceUntilIdle()

        assertTrue(result is Result.Success)
        assertEquals(contact, repository.state.value.snapshot?.contacts?.single())
        assertEquals(ContactError.Network, repository.state.value.error)
    }

    @Test
    fun changingAccountNeverReturnsPreviousSnapshot() = runTest {
        var account = "account-a"
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this, account = { account })
        repository.load()
        dataSource.contactsResult = Result.Success(listOf(contact.copy(name = "Account B")))
        account = "account-b"

        val result = repository.load()

        assertEquals("Account B", (result as Result.Success).data.contacts.single().name)
        assertEquals("account-b", repository.state.value.snapshot?.accountKey)
    }

    @Test
    fun localDateChangeMakesSnapshotStale() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        var date = LocalDate(2026, 8, 14)
        val dataSource = CountingDataSource()
        val repository = repository(dataSource, this, now = { now }, today = { date })
        repository.load()
        dataSource.resetCounts()
        date = LocalDate(2026, 8, 15)

        repository.load()
        advanceUntilIdle()

        assertEquals(5, dataSource.totalHomeReads)
    }

    private fun repository(
        dataSource: CountingDataSource,
        scope: CoroutineScope,
        account: suspend () -> String? = { "account-a" },
        now: () -> Instant = { Instant.parse("2026-08-14T12:00:00Z") },
        today: () -> LocalDate = { LocalDate(2026, 8, 14) },
    ) = InMemoryHomeRepository(
        contactDataSource = dataSource,
        accountKeyProvider = account,
        scope = scope,
        now = now,
        today = today,
    )
}

private val initialContacts = listOf(
    Contact(
        id = "c1",
        name = "Arfin",
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = "12:00:00",
        nextCheckInDate = "2099-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )
)
private val initialGroups = listOf(Group(id = "g1", name = "Family"))
private val initialMemberships = listOf(GroupMembership(contactId = "c1", groupId = "g1"))
private val initialCheckIns = listOf(
    CheckIn(
        id = "i1",
        contactId = "c1",
        checkedInAt = "2026-08-14T12:00:00Z",
    )
)

private class CountingDataSource : ContactDataSource by FakeContactDataSource(
    contacts = initialContacts,
    groups = initialGroups,
    memberships = initialMemberships,
    checkIns = initialCheckIns,
) {
    var contactsResult: Result<List<Contact>, ContactError> = Result.Success(initialContacts)
    var groupsResult: Result<List<Group>, ContactError> = Result.Success(initialGroups)
    var membershipsResult: Result<List<GroupMembership>, ContactError> = Result.Success(initialMemberships)
    var checkInsResult: Result<List<CheckIn>, ContactError> = Result.Success(initialCheckIns)

    var contactsReads = 0
    var groupsReads = 0
    var membershipsReads = 0
    var checkInsReads = 0

    val totalHomeReads: Int
        get() = contactsReads + groupsReads + membershipsReads + checkInsReads

    override suspend fun getContacts(): Result<List<Contact>, ContactError> {
        contactsReads++
        return contactsResult
    }

    override suspend fun getGroups(): Result<List<Group>, ContactError> {
        groupsReads++
        return groupsResult
    }

    override suspend fun getGroupMemberships(): Result<List<GroupMembership>, ContactError> {
        membershipsReads++
        return membershipsResult
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
        groupsReads = 0
        membershipsReads = 0
        checkInsReads = 0
    }
}

private inline fun <D, E, R> Result<D, E>.mapSnapshot(transform: (D) -> R): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> Result.Error(error)
}