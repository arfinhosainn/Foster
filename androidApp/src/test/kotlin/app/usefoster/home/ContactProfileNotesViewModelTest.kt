package app.usefoster.home

import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactError
import app.usefoster.home.domain.Note
import app.usefoster.home.presentation.contactprofile.ContactProfileAction
import app.usefoster.home.presentation.contactprofile.ContactProfileViewModel
import kotlinx.coroutines.CompletableDeferred
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
class ContactProfileNotesViewModelTest {

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

    private fun note(id: String, contactId: String = "c1") = Note(
        id = id,
        contactId = contactId,
        title = "Weekly Reflection",
        body = "A good week",
        createdAt = "2026-08-03T10:00:00Z",
    )

    @Test
    fun notesAreLoadedForContact() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                notes = listOf(note("n1"), note("n2")),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            assertEquals(listOf("n1", "n2"), viewModel.state.value.notes.map { it.id })
            assertNull(viewModel.state.value.notesError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun notesOnlyIncludeRequestedContact() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                notes = listOf(note("n1", "c1"), note("n2", "other")),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            assertEquals(listOf("n1"), viewModel.state.value.notes.map { it.id })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveNoteCreatesReloadsAndClosesSheet() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddNote)
            viewModel.onAction(ContactProfileAction.DraftTitleChanged("Coffee catchup"))
            viewModel.onAction(ContactProfileAction.DraftDescriptionChanged("Discuss the trip"))
            viewModel.onAction(ContactProfileAction.SaveNote)
            advanceUntilIdle()

            val call = dataSource.createNoteCalls.single()
            assertEquals("c1", call.contactId)
            assertEquals("Coffee catchup", call.title)
            assertEquals("Discuss the trip", call.body)

            val state = viewModel.state.value
            assertFalse(state.isAddNoteSheetOpen)
            assertEquals("", state.draftTitle)
            assertEquals("", state.draftDescription)
            assertTrue(state.notes.any { it.title == "Coffee catchup" })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteNoteRemovesAndReloads() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                notes = listOf(note("n1"), note("n2")),
            )
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.DeleteNote("n1"))
            advanceUntilIdle()

            assertEquals(listOf("n1"), dataSource.deletedNoteIds)
            assertEquals(listOf("n2"), viewModel.state.value.notes.map { it.id })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteNoteFailureKeepsNoteAndSetsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact()),
                notes = listOf(note("n1")),
            )
            dataSource.deleteNoteError = ContactError.Network
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.DeleteNote("n1"))
            advanceUntilIdle()

            assertEquals(listOf("n1"), dataSource.deletedNoteIds)
            assertEquals(listOf("n1"), viewModel.state.value.notes.map { it.id })
            assertNotNull(viewModel.state.value.notesError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveNoteWithBlankTitleIsNoOp() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddNote)
            viewModel.onAction(ContactProfileAction.DraftDescriptionChanged("no title"))
            viewModel.onAction(ContactProfileAction.SaveNote)
            advanceUntilIdle()

            assertEquals(0, dataSource.createNoteCalls.size)
            assertTrue(viewModel.state.value.isAddNoteSheetOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun getNotesFailureSetsNotesError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            dataSource.notesError = ContactError.Unknown("boom")
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.notes.isEmpty())
            assertNotNull(viewModel.state.value.notesError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveNoteFailureKeepsSheetOpenAndSetsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            dataSource.createNoteError = ContactError.Network
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddNote)
            viewModel.onAction(ContactProfileAction.DraftTitleChanged("Title"))
            viewModel.onAction(ContactProfileAction.SaveNote)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.isAddNoteSheetOpen)
            assertFalse(state.isSavingNote)
            assertNotNull(state.notesError)
            assertTrue(dataSource.notes.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun concurrentSavesAreBlocked() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact()))
            dataSource.createNoteGate = CompletableDeferred()
            val viewModel = ContactProfileViewModel("c1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(ContactProfileAction.OpenAddNote)
            viewModel.onAction(ContactProfileAction.DraftTitleChanged("Title"))
            viewModel.onAction(ContactProfileAction.SaveNote)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isSavingNote)
            assertEquals(1, dataSource.createNoteCalls.size)

            viewModel.onAction(ContactProfileAction.SaveNote)
            assertEquals(1, dataSource.createNoteCalls.size)

            dataSource.createNoteGate?.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, dataSource.createNoteCalls.size)
            assertFalse(viewModel.state.value.isAddNoteSheetOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
