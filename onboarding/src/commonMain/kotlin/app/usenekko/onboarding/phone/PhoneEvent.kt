package app.usenekko.onboarding.phone

sealed interface PhoneEvent {
    data class NavigateToCodeVerification(val phoneNumber: String) : PhoneEvent
    data object NavigateBack : PhoneEvent
    data object NavigateSkip : PhoneEvent
}
