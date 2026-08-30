package app.usefoster.onboarding.contact

import androidx.compose.runtime.Composable

@Composable
expect fun rememberContactPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit
