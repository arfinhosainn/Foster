package app.usefoster.onboarding

import app.usefoster.onboarding.dayreminder.ReminderAction
import app.usefoster.onboarding.dayreminder.ReminderOptions
import app.usefoster.onboarding.dayreminder.ReminderViewModel
import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.onboarding.domain.ReminderFrequency
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderViewModelTest {

    @Test
    fun unresolvedFrequencyDefaultsToDailyAndAdvancesOnNext() {
        val gate = CompletableDeferred<Unit>()
        val draftStore = OnboardingDraftStore(GatedDraftDataSource(OnboardingDraft(), gate))
        val viewModel = ReminderViewModel(draftStore)

        // Given no selection was persisted, the screen defaults to Daily on init.
        assertEquals(ReminderOptions.DAILY, viewModel.state.value.selectedOption)
        assertEquals(ReminderFrequency.Daily, draftStore.draft.value.reminderFrequency)

        // Tapping Next without touching an option must still persist Daily.
        viewModel.onNextClicked()
        assertEquals(ReminderFrequency.Daily, draftStore.draft.value.reminderFrequency)
        assertEquals(OnboardingStep.TimeReminder, draftStore.draft.value.currentStep)

        gate.complete(Unit)
    }

    @Test
    fun selectingWeeklyPersistsWeeklyAndSurvivesNext() {
        val gate = CompletableDeferred<Unit>()
        val draftStore = OnboardingDraftStore(GatedDraftDataSource(OnboardingDraft(), gate))
        val viewModel = ReminderViewModel(draftStore)

        viewModel.onAction(ReminderAction.SelectOption(ReminderOptions.WEEKLY))

        assertEquals(ReminderOptions.WEEKLY, viewModel.state.value.selectedOption)
        assertEquals(ReminderFrequency.Weekly, draftStore.draft.value.reminderFrequency)

        viewModel.onNextClicked()
        assertEquals(ReminderFrequency.Weekly, draftStore.draft.value.reminderFrequency)
        assertEquals(OnboardingStep.TimeReminder, draftStore.draft.value.currentStep)

        gate.complete(Unit)
    }
}

private class GatedDraftDataSource(
    private val base: OnboardingDraft,
    private val gate: CompletableDeferred<Unit>,
) : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> {
        gate.await()
        return Result.Success(base)
    }

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)
}