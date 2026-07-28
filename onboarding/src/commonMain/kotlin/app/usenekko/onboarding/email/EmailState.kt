package app.usenekko.onboarding.email

data class EmailState(
    val email: String = "",
    val isContinueEnabled: Boolean = false,
)
