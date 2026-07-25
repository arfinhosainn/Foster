package app.usenekko.onboarding.photo

data class PickPhotoState(
    val profilePhotoBytes: ByteArray? = null,
) {
    val hasPhoto: Boolean get() = profilePhotoBytes != null
}
