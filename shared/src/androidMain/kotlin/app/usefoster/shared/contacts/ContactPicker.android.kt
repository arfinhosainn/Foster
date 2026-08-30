package app.usefoster.shared.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
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
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                try {
                    contactPicker.launch(null)
                } catch (error: Exception) {
                    println("ContactPicker[Android]: picker launch failed")
                    onPermissionDenied()
                }
            } else {
                onPermissionDenied()
            }
        },
    )

    return remember(context, contactPicker, permissionLauncher) {
        {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    contactPicker.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            } catch (error: Exception) {
                println("ContactPicker[Android]: permission or picker launch failed")
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
            phoneNumber = readPhoneNumber(context, contactUri),
        )
    }
}

/**
 * Reads the contact's phone number so brainstorm topics can hand off to the
 * SMS app with the recipient pre-filled. Prefers the mobile number, falls back
 * to the first number on record.
 */
private fun readPhoneNumber(context: Context, contactUri: Uri): String? {
    val contactId = contactUri.lastPathSegment ?: return null
    return context.contentResolver.query(
        Phone.CONTENT_URI,
        arrayOf(Phone.NUMBER, Phone.TYPE),
        "${Phone.CONTACT_ID} = ?",
        arrayOf(contactId),
        null,
    )?.use { cursor ->
        var fallback: String? = null
        while (cursor.moveToNext()) {
            val number = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER))
                ?.trim()
                .orEmpty()
            if (number.isEmpty()) continue
            val type = cursor.getInt(cursor.getColumnIndexOrThrow(Phone.TYPE))
            if (type == Phone.TYPE_MOBILE) return@use number
            if (fallback == null) fallback = number
        }
        fallback
    }
}

private fun loadContactPhoto(context: Context, photoUri: Uri): ImageBitmap? =
    runCatching {
        context.contentResolver.openInputStream(photoUri)?.use { input ->
            BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    }.getOrNull()