package app.usenekko.onboarding.name

sealed interface NameEvent {
    data object NavigateToNext : NameEvent
    data object NavigateBack : NameEvent
    data object NavigateSkip : NameEvent
}
