package app.usenekko.onboarding

import app.usenekko.onboarding.dayreminder.ReminderAction
import app.usenekko.onboarding.dayreminder.ReminderOptions
import app.usenekko.onboarding.dayreminder.ReminderViewModel
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingDraftStorageError
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderFrequency
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
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