package app.usenekko.onboarding.photo

sealed interface PickPhotoAction {
    data class OnPhotoSelected(val bytes: ByteArray) : PickPhotoAction
    data object OnContinueClick : PickPhotoAction
}
