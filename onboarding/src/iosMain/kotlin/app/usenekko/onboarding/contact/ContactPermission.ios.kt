package app.usenekko.onboarding.contact

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberContactPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    return remember(onGranted, onDenied) {
        {
            CNContactStore().requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (granted) onGranted() else onDenied()
                }
            }
        }
    }
}
