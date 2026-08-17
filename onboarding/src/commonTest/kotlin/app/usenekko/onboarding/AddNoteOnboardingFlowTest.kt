package app.usenekko.onboarding

import app.usenekko.onboarding.addnote.AddNoteAction
import app.usenekko.onboarding.addnote.AddNoteViewModel
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.NoteDraft
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddNoteOnboardingFlowTest {

    @Test
    fun nextMovesToNotificationBeforeOnboardingIsSubmitted() = runBlocking {
        val draftStore = createDraftStore()
        val viewModel = AddNoteViewModel(draftStore)
        waitForDraftInitialization(draftStore)

        viewModel.onNavigateToNext()

        assertEquals(OnboardingStep.Notification, draftStore.draft.value.currentStep)
    }

    @Test
    fun skipMovesToNotificationBeforeOnboardingIsSubmitted() = runBlocking {
        val draftStore = createDraftStore()
        val viewModel = AddNoteViewModel(draftStore)
        waitForDraftInitialization(draftStore)

        viewModel.onSkip()

        assertEquals(OnboardingStep.Notification, draftStore.draft.value.currentStep)
    }

    @Test
    fun editingNoteFillsSheetAndUpdatesExistingNote() = runBlocking {
        val note = NoteDraft(
            id = "note_0",
            title = "First note",
            body = "Original content",
        )
        val draftStore = createDraftStore(
            OnboardingDraft(
                notes = listOf(note),
                currentStep = OnboardingStep.AddNote,
            ),
        )
        val viewModel = AddNoteViewModel(draftStore)
        waitForDraftInitialization(draftStore)
        while (viewModel.state.value.notes.isEmpty()) {
            delay(1)
        }

        viewModel.onAction(AddNoteAction.EditClicked(note.id))

        assertTrue(viewModel.state.value.isBottomSheetVisible)
        assertEquals(note.title, viewModel.state.value.draftTitle)
        assertEquals(note.body, viewModel.state.value.draftDescription)

        viewModel.onAction(AddNoteAction.DraftTitleChanged("Updated note"))
        viewModel.onAction(AddNoteAction.DraftDescriptionChanged("Updated content"))
        viewModel.onAction(AddNoteAction.SaveClicked)

        assertEquals(1, draftStore.draft.value.notes.size)
        assertEquals(note.id, draftStore.draft.value.notes.single().id)
        assertEquals("Updated note", draftStore.draft.value.notes.single().title)
        assertEquals("Updated content", draftStore.draft.value.notes.single().body)
    }

    private suspend fun waitForDraftInitialization(draftStore: OnboardingDraftStore) {
        while (draftStore.draft.value.currentStep != OnboardingStep.AddNote) {
            delay(1)
        }
    }

    private fun createDraftStore(initialDraft: OnboardingDraft = OnboardingDraft()): OnboardingDraftStore {
        return OnboardingDraftStore(
            object : OnboardingDraftLocalDataSource {
                override suspend fun getDraft(): OnboardingDraft {
                    return initialDraft
                }

                override suspend fun saveDraft(draft: OnboardingDraft) = Unit

                override suspend fun clearDraft() = Unit
            },
        )
    }
}