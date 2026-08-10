package app.usenekko.shared.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat

@Composable
actual fun rememberContactPicker(
    onContactSelected: (ImportedContact) -> Unit,
    onPermissionDenied: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            uri?.let { readContact(context, it) }?.let(onContactSelected)
        },
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                contactPicker.launch(null)
            } else {
                onPermissionDenied()
            }
        },
    )

    return remember(context, contactPicker, permissionLauncher) {
        {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                contactPicker.launch(null)
            } else {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
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