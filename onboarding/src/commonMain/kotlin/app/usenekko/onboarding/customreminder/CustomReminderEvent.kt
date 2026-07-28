package app.usenekko.onboarding.customreminder

sealed interface CustomReminderEvent {
    data object NavigateToNext : CustomReminderEvent
    data object NavigateBack : CustomReminderEvent
    data object NavigateSkip : CustomReminderEvent
}
