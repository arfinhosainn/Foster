package app.usenekko.onboarding.customreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.onboarding.domain.CustomReminderDraft
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderFrequency
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CustomReminderViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        CustomReminderState(reminders = draftStore.draft.value.customReminders.map { it.toReminderItem() })
    )
    val state = _state.asStateFlow()

    private val _events = Channel<CustomReminderEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.CustomReminder) }
    }

    fun onAction(action: CustomReminderAction) {
        when (action) {
            is CustomReminderAction.AddClicked -> {
                _state.value = _state.value.copy(isBottomSheetVisible = true)
            }
            is CustomReminderAction.BottomSheetDismissed -> {
                _state.value = _state.value.copy(isBottomSheetVisible = false)
            }
            is CustomReminderAction.DraftTitleChanged -> {
                _state.value = _state.value.copy(draftTitle = action.title)
            }
            is CustomReminderAction.DraftDescriptionChanged -> {
                _state.value = _state.value.copy(draftDescription = action.description)
            }
            is CustomReminderAction.DraftRecurrenceChanged -> {
                _state.value = _state.value.copy(draftRecurrence = action.recurrence)
            }
            is CustomReminderAction.DraftDateChanged -> {
                _state.value = _state.value.copy(draftDate = action.date)
            }
            is CustomReminderAction.SaveReminderClicked -> {
                val s = _state.value
                val newItem = CustomReminderDraft(
                    id = "rem_${draftStore.draft.value.customReminders.size}",
                    title = s.draftTitle.ifEmpty { "New Reminder" },
                    description = s.draftDescription,
                    recurrence = s.draftRecurrence.toReminderFrequency(),
                )
                draftStore.update {
                    it.copy(
                        customReminders = it.customReminders + newItem,
                        currentStep = OnboardingStep.CustomReminder,
                    )
                }
                _state.value = s.copy(
                    reminders = s.reminders + newItem.toReminderItem(),
                    isBottomSheetVisible = false,
                    draftTitle = "",
                    draftDescription = "",
                    draftRecurrence = "None",
                    draftDate = "Choose Date",
                )
            }
        }
    }

    fun onNextClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.AddNote) }
        viewModelScope.launch { _events.send(CustomReminderEvent.NavigateToNext) }
    }

    fun onBackClicked() {
        viewModelScope.launch { _events.send(CustomReminderEvent.NavigateBack) }
    }

    fun onSkipClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.AddNote) }
        viewModelScope.launch { _events.send(CustomReminderEvent.NavigateSkip) }
    }
}

private fun CustomReminderDraft.toReminderItem(): ReminderItem = ReminderItem(
    id = id,
    title = title,
    description = description,
    recurrence = recurrence.toUiLabel(),
    date = dateEpochMillis?.toString() ?: "Choose Date",
)

private fun ReminderFrequency.toUiLabel(): String = when (this) {
    ReminderFrequency.Daily -> "Daily"
    ReminderFrequency.Weekly -> "Weekly"
    ReminderFrequency.BiWeekly -> "Bi-weekly"
    ReminderFrequency.Monthly -> "Monthly"
    ReminderFrequency.SemiAnnually -> "Semi-annually"
    ReminderFrequency.Annually -> "Yearly"
    ReminderFrequency.None -> "None"
}

private fun String.toReminderFrequency(): ReminderFrequency = when (this) {
    "Daily" -> ReminderFrequency.Daily
    "Weekly" -> ReminderFrequency.Weekly
    "Bi-weekly" -> ReminderFrequency.BiWeekly
    "Monthly" -> ReminderFrequency.Monthly
    "Semi-annually" -> ReminderFrequency.SemiAnnually
    "Yearly", "Annually" -> ReminderFrequency.Annually
    else -> ReminderFrequency.None
}
