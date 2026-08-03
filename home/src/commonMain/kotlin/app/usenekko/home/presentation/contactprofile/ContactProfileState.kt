package app.usenekko.home.presentation.contactprofile

import app.usenekko.home.domain.Contact

enum class ContactProfileAction {
    ToggleRelationshipInfo,
    CheckIn,
}

data class ContactProfileState(
    val isLoading: Boolean = true,
    val contact: Contact? = null,
    val daysUntilNextCheckIn: Int = 0,
    val isRelationshipInfoOpen: Boolean = false,
    val isCheckingIn: Boolean = false,
    val checkInError: String? = null,
)
