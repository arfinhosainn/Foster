package app.usefoster.onboarding.customreminder

sealed interface CustomReminderAction {
    data object AddClicked : CustomReminderAction
    data class EditClicked(val reminderId: String) : CustomReminderAction
    data class DeleteReminderClicked(val reminderId: String) : CustomReminderAction
    data object BottomSheetDismissed : CustomReminderAction
    data class DraftTitleChanged(val title: String) : CustomReminderAction
    data class DraftDescriptionChanged(val description: String) : CustomReminderAction
    data class DraftRecurrenceChanged(val recurrence: String) : CustomReminderAction
    data class DraftDateChanged(val date: String, val dateEpochMillis: Long) : CustomReminderAction
    data object SaveReminderClicked : CustomReminderAction
}
