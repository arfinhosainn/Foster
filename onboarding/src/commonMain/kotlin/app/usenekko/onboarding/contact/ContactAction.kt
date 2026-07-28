package app.usenekko.onboarding.contact

sealed interface ContactAction {
    data class ContactNameChanged(val value: String) : ContactAction
    data class AvatarSelected(val index: Int) : ContactAction
    data object ImportClicked : ContactAction
    data object NextClicked : ContactAction
    data object BackClicked : ContactAction
    data object SkipClicked : ContactAction
}
