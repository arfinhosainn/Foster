package app.usenekko.onboarding.notification

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onStatusChanged: (Boolean) -> Unit,
): () -> Unit
