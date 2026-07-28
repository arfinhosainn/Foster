package app.usenekko.onboarding.timereminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderTimeDraft
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TimeReminderViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(draftStore.draft.value.reminderTime.toTimeReminderState())
    val state: StateFlow<TimeReminderState> = _state.asStateFlow()

    private val _events = Channel<TimeReminderEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.TimeReminder) }
    }

    fun onAction(action: TimeReminderAction) {
        when (action) {
            is TimeReminderAction.ScrollToMinute -> {
                val hour = if (action.totalMinutes / 60 == 0) 12 else action.totalMinutes / 60
                val minute = action.totalMinutes % 60
                _state.update {
                    it.copy(selectedHour = hour, selectedMinute = minute)
                }
                draftStore.update {
                    it.copy(
                        reminderTime = _state.value.toReminderTimeDraft(),
                        currentStep = OnboardingStep.TimeReminder,
                    )
                }
            }
            is TimeReminderAction.ToggleAmPm -> {
                _state.update { it.copy(isAm = action.isAm) }
                draftStore.update {
                    it.copy(
                        reminderTime = _state.value.toReminderTimeDraft(),
                        currentStep = OnboardingStep.TimeReminder,
                    )
                }
            }
        }
    }

    fun onNextClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.CustomReminder) }
        sendEvent(TimeReminderEvent.NavigateToNext)
    }

    fun onBackClicked() {
        sendEvent(TimeReminderEvent.NavigateBack)
    }

    fun onSkipClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.CustomReminder) }
        sendEvent(TimeReminderEvent.NavigateSkip)
    }

    private fun sendEvent(event: TimeReminderEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private fun ReminderTimeDraft?.toTimeReminderState(): TimeReminderState {
    if (this == null) return TimeReminderState()
    val periodHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return TimeReminderState(
        selectedHour = periodHour,
        selectedMinute = minute,
        isAm = hour < 12,
    )
}

private fun TimeReminderState.toReminderTimeDraft(): ReminderTimeDraft {
    val hour24 = when {
        isAm && selectedHour == 12 -> 0
        isAm -> selectedHour
        selectedHour == 12 -> 12
        else -> selectedHour + 12
    }
    return ReminderTimeDraft(hour = hour24, minute = selectedMinute)
}
