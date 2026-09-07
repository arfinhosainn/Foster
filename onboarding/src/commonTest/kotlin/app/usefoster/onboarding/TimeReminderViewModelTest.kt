package app.usefoster.onboarding

import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.onboarding.domain.ReminderTimeDraft
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import app.usefoster.onboarding.timereminder.TimeReminderAction
import app.usefoster.onboarding.timereminder.TimeReminderViewModel
import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeReminderViewModelTest {

    @Test
    fun scrollDialTimeIsStoredInDraft() {
        val draftStore = OnboardingDraftStore(TimeReminderInMemoryDraftDataSource())
        val viewModel = TimeReminderViewModel(draftStore)

        viewModel.onAction(TimeReminderAction.ScrollToMinute(totalMinutes = 9 * 60 + 5))

        assertEquals(9, viewModel.state.value.selectedHour)
        assertEquals(5, viewModel.state.value.selectedMinute)
        assertEquals(ReminderTimeDraft(hour = 9, minute = 5), draftStore.draft.value.reminderTime)
    }
}

private class TimeReminderInMemoryDraftDataSource : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> =
        Result.Success(OnboardingDraft())

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)
}