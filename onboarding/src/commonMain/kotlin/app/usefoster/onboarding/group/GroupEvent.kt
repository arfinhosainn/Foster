package app.usefoster.onboarding.group

sealed interface GroupEvent {
    data object NavigateToNext : GroupEvent
    data object NavigateBack : GroupEvent
}
