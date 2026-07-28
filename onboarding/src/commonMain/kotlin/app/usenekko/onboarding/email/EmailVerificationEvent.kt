package app.usenekko.onboarding.email

sealed interface EmailVerificationEvent {
    data object NavigateToNext : EmailVerificationEvent
    data object NavigateBack : EmailVerificationEvent
    data object NavigateSkip : EmailVerificationEvent
}
