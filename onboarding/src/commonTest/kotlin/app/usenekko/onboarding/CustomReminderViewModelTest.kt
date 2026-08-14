package app.usenekko.onboarding

import app.usenekko.onboarding.customreminder.CustomReminderAction
import app.usenekko.onboarding.customreminder.CustomReminderViewModel
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomReminderViewModelTest {

    @Test
    fun selectedDateIsSavedOnReminder() {
        val draftStore = OnboardingDraftStore(InMemoryDraftDataSource())
        val viewModel = CustomReminderViewModel(draftStore)

        viewModel.onAction(CustomReminderAction.DraftDateChanged("Aug 14, 2026", 1_000L))
        viewModel.onAction(CustomReminderAction.SaveReminderClicked)

        assertEquals("Jan 1, 1970", viewModel.state.value.reminders.single().date)
    }
}

private class InMemoryDraftDataSource : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): OnboardingDraft = OnboardingDraft()

    override suspend fun saveDraft(draft: OnboardingDraft) = Unit

    override suspend fun clearDraft() = Unit
}