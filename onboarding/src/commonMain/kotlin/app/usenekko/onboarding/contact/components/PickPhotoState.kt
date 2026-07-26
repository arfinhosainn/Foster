package app.usenekko.onboarding.contact.components

data class PickPhotoState(
    val profilePhotoBytes: ByteArray? = null,
) {
    val hasPhoto: Boolean get() = profilePhotoBytes != null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PickPhotoState

        if (!profilePhotoBytes.contentEquals(other.profilePhotoBytes)) return false
        if (hasPhoto != other.hasPhoto) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profilePhotoBytes?.contentHashCode() ?: 0
        result = 31 * result + hasPhoto.hashCode()
        return result
    }
}
