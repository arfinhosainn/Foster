package app.usenekko.onboarding.phone

data class PhoneState(
    val phoneNumber: String = "",
    val isContinueEnabled: Boolean = false,
)
