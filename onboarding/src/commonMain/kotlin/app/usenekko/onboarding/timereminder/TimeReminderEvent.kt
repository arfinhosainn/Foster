package app.usenekko.onboarding.timereminder

sealed interface TimeReminderEvent {
    data object NavigateToNext : TimeReminderEvent
    data object NavigateBack : TimeReminderEvent
    data object NavigateSkip : TimeReminderEvent
}
