package app.usenekko.shared.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Contacts.CNContact
import platform.Contacts.CNContactFamilyNameKey
import platform.Contacts.CNContactGivenNameKey
import platform.Contacts.CNContactImageDataKey
import platform.Contacts.CNContactPhoneNumbersKey
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber
import platform.ContactsUI.CNContactPickerDelegateProtocol
import platform.ContactsUI.CNContactPickerViewController
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberContactPicker(
    onContactSelected: (ImportedContact) -> Unit,
    onPermissionDenied: () -> Unit,
): () -> Unit {
    val launcher = remember(onContactSelected, onPermissionDenied) {
        {
            val presenter = currentViewController()
            if (presenter == null) {
                onPermissionDenied()
            } else {
                val delegate = ContactPickerDelegate(onContactSelected)
                activeDelegate = delegate
                val picker = CNContactPickerViewController()
                picker.delegate = delegate
                picker.displayedPropertyKeys = listOf(
                    CNContactGivenNameKey,
                    CNContactFamilyNameKey,
                    CNContactImageDataKey,
                    CNContactPhoneNumbersKey,
                )
                presenter.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
    return launcher
}

@OptIn(ExperimentalForeignApi::class)
private var activeDelegate: ContactPickerDelegate? = null

@OptIn(ExperimentalForeignApi::class)
private class ContactPickerDelegate(
    private val onContactSelected: (ImportedContact) -> Unit,
) : NSObject(), CNContactPickerDelegateProtocol {
    override fun contactPicker(
        picker: CNContactPickerViewController,
        didSelectContact: CNContact,
    ) {
        val name = listOf(didSelectContact.givenName, didSelectContact.familyName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { didSelectContact.organizationName }
            .trim()
        if (name.isNotEmpty()) {
            onContactSelected(
                ImportedContact(
                    name = name,
                    photo = didSelectContact.imageData?.toImageBitmap()
                        ?: didSelectContact.thumbnailImageData?.toImageBitmap(),
                    phoneNumber = didSelectContact.primaryPhoneNumber(),
                ),
            )
        }
        activeDelegate = null
    }

    override fun contactPickerDidCancel(picker: CNContactPickerViewController) {
        activeDelegate = null
    }
}

/**
 * Prefers the mobile/iPhone number, falls back to the first number on record.
 */
private fun CNContact.primaryPhoneNumber(): String? {
    val entries = phoneNumbers.orEmpty()
        .filterIsInstance<CNLabeledValue>()
        .mapNotNull { labeledValue ->
            val number = (labeledValue.value as? CNPhoneNumber)?.stringValue
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            number to ((labeledValue.label as? String).orEmpty())
        }
    return entries.firstOrNull { (_, label) ->
        label.contains("mobile", ignoreCase = true) || label.contains("iphone", ignoreCase = true)
    }?.first
        ?: entries.firstOrNull()?.first
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toImageBitmap(): ImageBitmap? =
    runCatching {
        val bytes = ByteArray(length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toImageBitmap.bytes, length)
        }
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.getOrNull()

@OptIn(ExperimentalForeignApi::class)
private fun currentViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.windows
        .filterIsInstance<UIWindow>()
        .firstOrNull { it.isKeyWindow() }
        ?: UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().firstOrNull()
    var current = window?.rootViewController
    while (current?.presentedViewController != null) {
        current = current.presentedViewController
    }
    return current
}