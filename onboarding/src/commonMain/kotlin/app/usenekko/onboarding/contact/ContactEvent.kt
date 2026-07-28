package app.usenekko.onboarding.contact

sealed interface ContactEvent {
    data object NavigateToNext : ContactEvent
    data object NavigateBack : ContactEvent
    data object NavigateSkip : ContactEvent
}
