package app.usenekko.onboarding.email

data class EmailVerificationState(
    val code: String = "",
    val isVerifying: Boolean = false,
)
