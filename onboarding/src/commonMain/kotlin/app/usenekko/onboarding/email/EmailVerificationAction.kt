package app.usenekko.onboarding.email

sealed interface EmailVerificationAction {
    data class CodeChanged(val code: String) : EmailVerificationAction
    data object VerifyClicked : EmailVerificationAction
    data object Done : EmailVerificationAction
    data object BackClicked : EmailVerificationAction
    data object SkipClicked : EmailVerificationAction
}
