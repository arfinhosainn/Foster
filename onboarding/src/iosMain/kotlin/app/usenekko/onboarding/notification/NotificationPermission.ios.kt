package app.usenekko.onboarding.notification

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    return {
        // TODO: iOS notification permission via UNUserNotificationCenter
        onGranted()
    }
}
