package app.usenekko.home

import app.usenekko.home.domain.Contact
import app.usenekko.home.presentation.contactprofile.ContactProfileAction
import app.usenekko.home.presentation.contactprofile.ContactProfileViewModel
import app.usenekko.shared.domain.AccountProfile
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactProfileViewModelTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun contact(
        id: String = "c1",
        frequency: String = "daily",
        next: String = today.minus(DatePeriod(days = 1)).toString(),
        last: String? = null,
        streak: Int = 0,
    ) = Contact(
        id = id,
        name = "Liam",
        avatarColor = "#007AFF",
        checkInFrequency = frequency,
        reminderTime = "07:30:00",
        nextCheckInDate = next,
        lastCheckInDate = last,
        streakCount = streak,
    )

    @Test
    fun daysUntilNextCheckInIsComputed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact(next = today.plus(DatePeriod(days = 5)).toString())),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(5, viewModel.state.value.daysUntilNextCheckIn)
            assertEquals("Liam", viewModel.state.value.contact?.name)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun outstandingContactShowsZeroDays() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(0, viewModel.state.value.daysUntilNextCheckIn)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun signedInUserAvatarIsLoadedForRelationshipSheet() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val profileDataSource = FakeProfileDataSource(
                profile = AccountProfile(
                    fullName = "Jane Bell",
                    displayName = null,
                    avatarUrl = null,
                    selectedAvatarId = "2",
                    createdAt = "2026-01-15T10:00:00Z",
                ),
            )
            val viewModel = ContactProfileViewModel(
                "c1",
                dataSource,
                ReminderScheduler(),
                profileDataSource,
            )
            advanceUntilIdle()

            assertEquals("2", viewModel.state.value.userSelectedAvatarId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun toggleRelationshipInfoFlipsFlag() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isRelationshipInfoOpen)
            viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo)
            assertTrue(viewModel.state.value.isRelationshipInfoOpen)
            viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo)
            assertFalse(viewModel.state.value.isRelationshipInfoOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun checkInReusesLogCheckInAndUpdatesContact() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.CheckIn)
            advanceUntilIdle()

            val call = dataSource.logCheckInCalls.single()
            assertEquals("c1", call.contactId)
            assertEquals(today.toString(), call.lastCheckInDate)
            assertEquals(today.plus(DatePeriod(days = 1)).toString(), call.nextCheckInDate)
            assertEquals(1, call.streakCount)

            assertEquals(today.toString(), viewModel.state.value.contact?.lastCheckInDate)
            assertEquals(1, viewModel.state.value.contact?.streakCount)
            assertEquals(1, viewModel.state.value.daysUntilNextCheckIn)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sameDayDoubleCheckInIsIdempotent() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.CheckIn)
            advanceUntilIdle()

            // After the first check-in the contact's next date is in the future, so
            // a second tap on the same day must not insert another row.
            viewModel.onAction(ContactProfileAction.CheckIn)
            advanceUntilIdle()

            assertEquals(1, dataSource.logCheckInCalls.size)
            assertEquals(1, dataSource.checkIns.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun upToDateContactCheckInIsNoOp() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact(next = today.plus(DatePeriod(days = 7)).toString())),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.CheckIn)
            advanceUntilIdle()

            assertEquals(0, dataSource.logCheckInCalls.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun concurrentCheckInsAreBlocked() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            dataSource.checkInGate = CompletableDeferred()
            val viewModel = ContactProfileViewModel("c1", dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.CheckIn)
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)

            viewModel.onAction(ContactProfileAction.CheckIn)
            assertEquals(1, dataSource.logCheckInCalls.size)

            dataSource.checkInGate?.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun missingContactLeavesStateEmptyWithoutCrashing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = emptyList())
            val viewModel = ContactProfileViewModel("ghost", dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isLoading)
            assertEquals(null, viewModel.state.value.contact)

            // Still safe to interact — actions are no-ops, not crashes.
            viewModel.onAction(ContactProfileAction.CheckIn)
            advanceUntilIdle()
            assertEquals(0, dataSource.logCheckInCalls.size)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
