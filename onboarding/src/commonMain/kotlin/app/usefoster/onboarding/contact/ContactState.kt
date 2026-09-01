package app.usefoster.onboarding.contact

import androidx.compose.ui.graphics.ImageBitmap

data class ContactState(
    val contactName: String = "",
    val selectedAvatarIndex: Int? = null,
    val importedPhoto: ImageBitmap? = null,
    val showAvatarPicker: Boolean = false,
    /** Inline "name is required" error, shown under the name field. */
    val showNameError: Boolean = false,
)
