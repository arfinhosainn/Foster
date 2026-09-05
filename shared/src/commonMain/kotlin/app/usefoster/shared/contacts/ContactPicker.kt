package app.usefoster.shared.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

data class ImportedContact(
    val name: String,
    val photo: ImageBitmap? = null,
)

@Composable
expect fun rememberContactPicker(
    onContactSelected: (ImportedContact) -> Unit,
    onPermissionDenied: () -> Unit,
): () -> Unit