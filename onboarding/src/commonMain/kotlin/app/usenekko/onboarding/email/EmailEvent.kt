package app.usenekko.onboarding.email

sealed interface EmailEvent {
    data class NavigateToEmailVerification(val email: String) : EmailEvent
    data object NavigateBack : EmailEvent
    data object NavigateSkip : EmailEvent
}
