package app.usenekko.onboarding.customreminder

sealed interface CustomReminderAction {
    data object AddClicked : CustomReminderAction
    data object BottomSheetDismissed : CustomReminderAction
    data class DraftTitleChanged(val title: String) : CustomReminderAction
    data class DraftDescriptionChanged(val description: String) : CustomReminderAction
    data class DraftRecurrenceChanged(val recurrence: String) : CustomReminderAction
    data class DraftDateChanged(val date: String) : CustomReminderAction
    data object SaveReminderClicked : CustomReminderAction
}
