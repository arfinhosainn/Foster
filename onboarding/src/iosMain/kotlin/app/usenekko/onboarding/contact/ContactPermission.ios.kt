package app.usenekko.onboarding.contact

import androidx.compose.runtime.Composable

@Composable
actual fun rememberContactPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    return {
        // TODO: iOS contact permission via CNContactStore
        onGranted()
    }
}
