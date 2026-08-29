package app.usenekko.onboarding.contact

import app.usenekko.shared.contacts.ImportedContact

sealed interface ContactAction {
    data class ContactNameChanged(val value: String) : ContactAction
    data class AvatarSelected(val index: Int) : ContactAction
    data class ContactImported(val contact: ImportedContact) : ContactAction
    data object ImportFailed : ContactAction
    data object NextClicked : ContactAction
    data object BackClicked : ContactAction
    data object SkipClicked : ContactAction
}
