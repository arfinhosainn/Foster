package app.usefoster.onboarding.dayreminder

sealed interface ReminderEvent {
    data object NavigateToNext : ReminderEvent
    data object NavigateBack : ReminderEvent
}
