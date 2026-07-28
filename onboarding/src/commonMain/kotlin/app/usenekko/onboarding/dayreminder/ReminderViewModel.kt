package app.usenekko.onboarding.dayreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderFrequency
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ReminderState(
            selectedOption = draftStore.draft.value.reminderFrequency.toReminderOption()
        )
    )
    val state: StateFlow<ReminderState> = _state.asStateFlow()

    private val _events = Channel<ReminderEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.DayReminder) }
    }

    fun onAction(action: ReminderAction) {
        when (action) {
            is ReminderAction.SelectOption -> {
                _state.value = _state.value.copy(selectedOption = action.option)
                draftStore.update {
                    it.copy(
                        reminderFrequency = action.option.toReminderFrequency(),
                        currentStep = OnboardingStep.DayReminder,
                    )
                }
            }
        }
    }

    fun onNextClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.TimeReminder) }
        sendEvent(ReminderEvent.NavigateToNext)
    }

    fun onBackClicked() {
        sendEvent(ReminderEvent.NavigateBack)
    }

    private fun sendEvent(event: ReminderEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private fun ReminderFrequency?.toReminderOption(): String = when (this) {
    ReminderFrequency.Daily -> ReminderOptions.DAILY
    ReminderFrequency.Weekly -> ReminderOptions.WEEKLY
    ReminderFrequency.BiWeekly -> ReminderOptions.BI_WEEKLY
    ReminderFrequency.Monthly -> ReminderOptions.MONTHLY
    ReminderFrequency.SemiAnnually -> ReminderOptions.SEMI_ANNUALLY
    ReminderFrequency.Annually -> ReminderOptions.ANNUALLY
    ReminderFrequency.None, null -> ReminderOptions.DAILY
}

private fun String.toReminderFrequency(): ReminderFrequency = when (this) {
    ReminderOptions.DAILY -> ReminderFrequency.Daily
    ReminderOptions.WEEKLY -> ReminderFrequency.Weekly
    ReminderOptions.BI_WEEKLY -> ReminderFrequency.BiWeekly
    ReminderOptions.MONTHLY -> ReminderFrequency.Monthly
    ReminderOptions.SEMI_ANNUALLY -> ReminderFrequency.SemiAnnually
    ReminderOptions.ANNUALLY -> ReminderFrequency.Annually
    else -> ReminderFrequency.None
}
