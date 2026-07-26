package app.usenekko.onboarding.contact.components

sealed interface PickPhotoAction {
    data class OnPhotoSelected(val bytes: ByteArray) : PickPhotoAction
    data object OnContinueClick : PickPhotoAction
}
