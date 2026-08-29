package app.usenekko.onboarding

import app.usenekko.onboarding.customreminder.CustomReminderAction
import app.usenekko.onboarding.customreminder.CustomReminderState
import app.usenekko.onboarding.customreminder.CustomReminderViewModel
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingDraftStorageError
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomReminderViewModelTest {

    @Test
    fun selectedDateIsSavedOnReminder() {
        val draftStore = OnboardingDraftStore(InMemoryDraftDataSource())
        val viewModel = CustomReminderViewModel(draftStore)

        viewModel.onAction(CustomReminderAction.DraftDateChanged("Aug 14, 2026", 1_000L))
        viewModel.onAction(CustomReminderAction.SaveReminderClicked)

        assertEquals("Jan 1, 1970", viewModel.state.value.reminders.single().date)
    }

    @Test
    fun editingReminderFillsSheetAndUpdatesExistingReminder() {
        val draftStore = OnboardingDraftStore(InMemoryDraftDataSource())
        val viewModel = CustomReminderViewModel(draftStore)

        viewModel.onAction(CustomReminderAction.DraftTitleChanged("Birthday"))
        viewModel.onAction(CustomReminderAction.DraftDescriptionChanged("Call Mom"))
        viewModel.onAction(CustomReminderAction.DraftRecurrenceChanged("Yearly"))
        viewModel.onAction(CustomReminderAction.DraftDateChanged("Aug 14, 2026", 1_000L))
        viewModel.onAction(CustomReminderAction.SaveReminderClicked)
        val reminderId = viewModel.state.value.reminders.single().id

        viewModel.onAction(CustomReminderAction.EditClicked(reminderId))

        assertTrue(viewModel.state.value.isBottomSheetVisible)
        assertEquals("Birthday", viewModel.state.value.draftTitle)
        assertEquals("Call Mom", viewModel.state.value.draftDescription)
        assertEquals("Yearly", viewModel.state.value.draftRecurrence)
        assertEquals(1_000L, viewModel.state.value.draftDateEpochMillis)

        viewModel.onAction(CustomReminderAction.DraftTitleChanged("Updated Birthday"))
        viewModel.onAction(CustomReminderAction.SaveReminderClicked)

        assertEquals(1, viewModel.state.value.reminders.size)
        assertEquals(reminderId, viewModel.state.value.reminders.single().id)
        assertEquals("Updated Birthday", viewModel.state.value.reminders.single().title)
    }

    @Test
    fun editingUnknownReminderDoesNothing() {
        val draftStore = OnboardingDraftStore(InMemoryDraftDataSource())
        val viewModel = CustomReminderViewModel(draftStore)

        viewModel.onAction(CustomReminderAction.EditClicked("missing"))

        assertEquals(CustomReminderState(), viewModel.state.value)
    }
}

private class InMemoryDraftDataSource : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> =
        Result.Success(OnboardingDraft())

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)
}