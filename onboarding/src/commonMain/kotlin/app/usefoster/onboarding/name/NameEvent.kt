package app.usefoster.onboarding.name

sealed interface NameEvent {
    data object NavigateToNext : NameEvent
    data object NavigateBack : NameEvent
    data object NavigateSkip : NameEvent
    data object NameRequired : NameEvent
}
