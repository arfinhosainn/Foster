package app.usenekko.home

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.data.InMemoryAccountRepository
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.domain.forTodayCheckInList
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.shared.notifications.ReminderScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCheckInTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun contact(
        id: String,
        frequency: String = "daily",
        next: String = today.minus(DatePeriod(days = 1)).toString(),
    ) = Contact(
        id = id,
        name = "C$id",
        avatarColor = "#007AFF",
        checkInFrequency = frequency,
        reminderTime = "12:00:00",
        nextCheckInDate = next,
        lastCheckInDate = null,
        streakCount = 0,
    )

    @Test
    fun checkInFlipsOutstandingToUpToDate() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1")))
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(0, viewModel.state.value.upToDateCount)

            viewModel.checkIn("c1")
            advanceUntilIdle()

            val call = dataSource.logCheckInCalls.single()
            assertEquals("c1", call.contactId)
            assertEquals(today.toString(), call.lastCheckInDate)
            assertEquals(today.plus(DatePeriod(days = 1)).toString(), call.nextCheckInDate)
            assertEquals(1, call.streakCount)

            assertEquals(0, viewModel.state.value.outstandingCount)
            assertEquals(1, viewModel.state.value.upToDateCount)
            assertEquals(1, viewModel.state.value.checkIns.size)
            assertEquals("c1", viewModel.state.value.checkIns.first().contactId)
            assertEquals(null, viewModel.state.value.checkingInContactId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun todayCheckInEventDoesNotRemainOutstandingWhenContactCacheIsStale() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
                checkIns = listOf(CheckIn("ci1", "c1", "${today}T12:00:00Z")),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(0, viewModel.state.value.outstandingCount)
            assertEquals(1, viewModel.state.value.upToDateCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onlyOutstandingContactsGetAButtonState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val upToDate = contact("c1", next = today.plus(DatePeriod(days = 7)).toString())
            val outstanding = contact("c2", next = today.minus(DatePeriod(days = 2)).toString())
            val dataSource = FakeContactDataSource(contacts = listOf(upToDate, outstanding))
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(listOf("c1", "c2"), viewModel.state.value.contacts.map { it.id })
            assertEquals(1, viewModel.state.value.outstandingCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun todayCheckInListExcludesContactsDueLaterAndIncludesCompletedToday() {
        val completedToday = contact("completed", next = today.plus(DatePeriod(days = 7)).toString())
            .copy(lastCheckInDate = today.toString())
        val dueToday = contact("due")
        val dueLater = contact("later", next = today.plus(DatePeriod(days = 7)).toString())

        assertEquals(
            listOf("due", "completed"),
            listOf(completedToday, dueToday, dueLater).forTodayCheckInList(today).map { it.id },
        )
    }

    @Test
    fun stateContainsTotalCheckInsPerContact() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1"), contact("c2", next = today.plus(DatePeriod(days = 7)).toString())),
                checkIns = listOf(
                    CheckIn("ci1", "c1", "2026-07-01T12:00:00Z"),
                    CheckIn("ci2", "c1", "2026-07-15T12:00:00Z"),
                    CheckIn("ci3", "c2", "2026-07-20T12:00:00Z"),
                ),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(mapOf("c1" to 2, "c2" to 1), viewModel.state.value.checkInCounts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun concurrentCheckInsAreBlocked() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1"), contact("c2", next = today.minus(DatePeriod(days = 2)).toString())),
            )
            dataSource.checkInGate = CompletableDeferred()
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)

            viewModel.checkIn("c2")
            assertEquals(1, dataSource.logCheckInCalls.size)

            dataSource.checkInGate?.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun successfulCheckInInvalidatesAccountBadgeCache() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
            )
            val homeRepository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "account-a" },
                scope = this,
            )
            val accountRepository = InMemoryAccountRepository(
                profileDataSource = FakeProfileDataSource(),
                contactDataSource = dataSource,
                accountKeyProvider = { "account-a" },
                scope = this,
            )
            accountRepository.load()
            val viewModel = HomeViewModel(
                dataSource,
                ReminderScheduler(),
                homeRepository,
                accountRepository,
            )
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()
            val badgeReadsBeforeAccountRefresh = dataSource.getBadgesCalls
            accountRepository.load()
            advanceUntilIdle()

            assertEquals(badgeReadsBeforeAccountRefresh + 1, dataSource.getBadgesCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
