package app.usenekko.shared.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

data class ImportedContact(
    val name: String,
    val photo: ImageBitmap? = null,
    /** Primary phone number captured at import time — enables SMS hand-off. */
    val phoneNumber: String? = null,
)

@Composable
expect fun rememberContactPicker(
    onContactSelected: (ImportedContact) -> Unit,
    onPermissionDenied: () -> Unit,
): () -> Unit