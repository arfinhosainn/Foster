package app.usefoster.home

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactError
import app.usefoster.home.domain.Reminder
import app.usefoster.home.presentation.contactprofile.ContactProfileAction
import app.usefoster.home.presentation.contactprofile.ContactProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactProfileRemindersViewModelTest {

    private fun contact() = Contact(
        id = "c1",
        name = "Liam",
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = "07:30:00",
        nextCheckInDate = "2030-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )

    private fun reminder(id: String, contactId: String = "c1") = Reminder(
        id = id,
        contactId = contactId,
        title = "Birthday",
        description = "Send a card",
        recurrence = "annually",
        dateEpochMillis = 1000L,
    )

    @Test
    fun remindersAreLoadedForContact() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                reminders = listOf(reminder("r1"), reminder("r2", "other")),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            assertEquals(listOf("r1"), viewModel.state.value.reminders.map { it.id })
            assertNull(viewModel.state.value.remindersError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun checkInCountIsDerivedFromCheckIns() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                checkIns = listOf(
                    CheckIn("a", "c1", "2026-01-01T10:00:00Z", null),
                    CheckIn("b", "c1", "2026-02-01T10:00:00Z", null),
                    CheckIn("c", "other", "2026-03-01T10:00:00Z", null),
                ),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.checkInCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveReminderCreatesReloadsAndShowsReminderList() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddReminder)
            viewModel.onAction(ContactProfileAction.ReminderDraftTitleChanged("Anniversary"))
            viewModel.onAction(ContactProfileAction.ReminderDraftDescriptionChanged("Our date"))
            viewModel.onAction(ContactProfileAction.ReminderDraftRecurrenceChanged("weekly"))
            viewModel.onAction(ContactProfileAction.ReminderDraftDateChanged(123456789L))
            viewModel.onAction(ContactProfileAction.SaveReminder)
            advanceUntilIdle()

            val call = dataSource.createReminderCalls.single()
            assertEquals("c1", call.contactId)
            assertEquals("Anniversary", call.title)
            assertEquals("Our date", call.description)
            assertEquals("weekly", call.recurrence)
            assertEquals(123456789L, call.date)

            val state = viewModel.state.value
            assertFalse(state.isAddReminderSheetOpen)
            assertTrue(state.isReminderListSheetOpen)
            assertEquals("", state.reminderDraftTitle)
            assertEquals("", state.reminderDraftDescription)
            assertEquals("none", state.reminderDraftRecurrence)
            assertNull(state.reminderDraftDateEpochMillis)
            assertTrue(state.reminders.any { it.title == "Anniversary" })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun openingReminderListShowsTheReminderSheet() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = ContactProfileViewModel(
                "c1",
                FakeContactDataSource(contacts = listOf(contact())),
            )
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenReminderList)

            assertTrue(viewModel.state.value.isReminderListSheetOpen)
            viewModel.onAction(ContactProfileAction.CloseReminderList)
            assertFalse(viewModel.state.value.isReminderListSheetOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveReminderWithBlankTitleIsNoOp() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddReminder)
            viewModel.onAction(ContactProfileAction.ReminderDraftDescriptionChanged("no title"))
            viewModel.onAction(ContactProfileAction.SaveReminder)
            advanceUntilIdle()

            assertEquals(0, dataSource.createReminderCalls.size)
            assertTrue(viewModel.state.value.isAddReminderSheetOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun editReminderPrefillsDraftAndOpensSheet() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                reminders = listOf(reminder("r1")),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.EditReminder("r1"))

            val state = viewModel.state.value
            assertTrue(state.isAddReminderSheetOpen)
            assertEquals("Birthday", state.reminderDraftTitle)
            assertEquals("Send a card", state.reminderDraftDescription)
            assertEquals("annually", state.reminderDraftRecurrence)
            assertEquals(1000L, state.reminderDraftDateEpochMillis)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteReminderRemovesAndReloads() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                reminders = listOf(reminder("r1"), reminder("r2")),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.DeleteReminder("r1"))
            advanceUntilIdle()

            assertEquals(listOf("r1"), dataSource.deletedReminderIds)
            assertEquals(listOf("r2"), viewModel.state.value.reminders.map { it.id })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteReminderFailureSetsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                reminders = listOf(reminder("r1")),
            )
            dataSource.deleteReminderError = ContactError.Network
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.DeleteReminder("r1"))
            advanceUntilIdle()

            assertEquals(listOf("r1"), dataSource.deletedReminderIds)
            assertEquals(listOf("r1"), viewModel.state.value.reminders.map { it.id })
            assertNotNull(viewModel.state.value.remindersError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveReminderFailureKeepsSheetOpenAndSetsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            dataSource.createReminderError = ContactError.Network
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddReminder)
            viewModel.onAction(ContactProfileAction.ReminderDraftTitleChanged("Title"))
            viewModel.onAction(ContactProfileAction.SaveReminder)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.isAddReminderSheetOpen)
            assertFalse(state.isSavingReminder)
            assertNotNull(state.remindersError)
            assertTrue(dataSource.reminders.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }
}
