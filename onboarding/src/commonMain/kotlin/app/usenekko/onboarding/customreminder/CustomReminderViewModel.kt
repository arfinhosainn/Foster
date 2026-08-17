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
                _state.value = _state.value.copy(
                    isBottomSheetVisible = true,
                    draftTitle = "",
                    draftDescription = "",
                    draftRecurrence = "None",
                    draftDate = "Choose Date",
                    draftDateEpochMillis = null,
                    editingReminderId = null,
                )
            }
            is CustomReminderAction.EditClicked -> {
                val reminder = draftStore.draft.value.customReminders
                    .firstOrNull { it.id == action.reminderId }
                    ?: return
                _state.value = _state.value.copy(
                    isBottomSheetVisible = true,
                    draftTitle = reminder.title,
                    draftDescription = reminder.description,
                    draftRecurrence = reminder.recurrence.toUiLabel(),
                    draftDate = reminder.dateEpochMillis?.toReminderDate() ?: "Choose Date",
                    draftDateEpochMillis = reminder.dateEpochMillis,
                    editingReminderId = reminder.id,
                )
            }
            is CustomReminderAction.BottomSheetDismissed -> {
                _state.value = _state.value.copy(
                    isBottomSheetVisible = false,
                    draftTitle = "",
                    draftDescription = "",
                    draftRecurrence = "None",
                    draftDate = "Choose Date",
                    draftDateEpochMillis = null,
                    editingReminderId = null,
                )
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
                _state.value = _state.value.copy(
                    draftDate = action.date,
                    draftDateEpochMillis = action.dateEpochMillis,
                )
            }
            is CustomReminderAction.SaveReminderClicked -> {
                val s = _state.value
                val reminderId = s.editingReminderId
                    ?: "rem_${draftStore.draft.value.customReminders.size}"
                val newItem = CustomReminderDraft(
                    id = reminderId,
                    title = s.draftTitle.ifEmpty { "New Reminder" },
                    description = s.draftDescription,
                    recurrence = s.draftRecurrence.toReminderFrequency(),
                    dateEpochMillis = s.draftDateEpochMillis,
                )
                draftStore.update {
                    it.copy(
                        customReminders = if (s.editingReminderId == null) {
                            it.customReminders + newItem
                        } else {
                            it.customReminders.map { reminder ->
                                if (reminder.id == s.editingReminderId) newItem else reminder
                            }
                        },
                        currentStep = OnboardingStep.CustomReminder,
                    )
                }
                val updatedReminders = if (s.editingReminderId == null) {
                    s.reminders + newItem.toReminderItem()
                } else {
                    s.reminders.map { reminder ->
                        if (reminder.id == s.editingReminderId) newItem.toReminderItem() else reminder
                    }
                }
                _state.value = s.copy(
                    reminders = updatedReminders,
                    isBottomSheetVisible = false,
                    draftTitle = "",
                    draftDescription = "",
                    draftRecurrence = "None",
                    draftDate = "Choose Date",
                    draftDateEpochMillis = null,
                    editingReminderId = null,
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
    date = dateEpochMillis?.toReminderDate() ?: "Choose Date",
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
