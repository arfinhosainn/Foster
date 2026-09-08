package app.usefoster.shared.contacts

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.Contacts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberContactPicker(
    onContactSelected: (ImportedContact) -> Unit,
    onPermissionDenied: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    // Android system contact picker (ACTION_PICK via PickContact): the OS
    // returns exactly ONE contact's URI and needs NO permission. Compliant
    // with Google's Contact Picker policy — COMPLIANCE_TODO item 16.
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            try {
                val contact = readContact(context, uri)
                if (contact == null) {
                    onPermissionDenied()
                } else {
                    onContactSelected(contact)
                }
            } catch (error: Exception) {
                println("ContactPicker[Android]: contact read failed")
                onPermissionDenied()
            }
        },
    )

    return remember(context, contactPicker) {
        {
            try {
                contactPicker.launch(null)
            } catch (error: Exception) {
                println("ContactPicker[Android]: picker launch failed")
                onPermissionDenied()
            }
        }
    }
}

private fun readContact(context: Context, contactUri: Uri): ImportedContact? {
    val projection = arrayOf(Contacts.DISPLAY_NAME, Contacts.PHOTO_URI)
    return context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null

        val name = cursor.getString(cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME))
            ?.trim()
            .orEmpty()
        if (name.isEmpty()) return null

        val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(Contacts.PHOTO_URI))
        ImportedContact(
            name = name,
            photo = photoUri?.let { loadContactPhoto(context, Uri.parse(it)) },
        )
    }
}

private fun loadContactPhoto(context: Context, photoUri: Uri): ImageBitmap? =
    runCatching {
        context.contentResolver.openInputStream(photoUri)?.use { input ->
            BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    }.getOrNull()