package app.usenekko.onboarding

import app.usenekko.onboarding.addnote.AddNoteViewModel
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private suspend fun waitForDraftInitialization(draftStore: OnboardingDraftStore) {
        while (draftStore.draft.value.currentStep != OnboardingStep.AddNote) {
            delay(1)
        }
    }

    private fun createDraftStore(): OnboardingDraftStore {
        return OnboardingDraftStore(
            object : OnboardingDraftLocalDataSource {
                override suspend fun getDraft(): OnboardingDraft {
                    return OnboardingDraft(currentStep = OnboardingStep.AddNote)
                }

                override suspend fun saveDraft(draft: OnboardingDraft) = Unit

                override suspend fun clearDraft() = Unit
            },
        )
    }
}