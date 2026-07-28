package app.usenekko.onboarding.contact

data class ContactState(
    val contactName: String = "",
    val selectedAvatarIndex: Int? = null,
    val showAvatarPicker: Boolean = false,
)
