package app.usefoster.onboarding.notification

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

@Composable
actual fun rememberNotificationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onStatusChanged: (Boolean) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    LaunchedEffect(context) {
        onStatusChanged(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onStatusChanged(granted)
        if (granted) onGranted() else onDenied()
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onStatusChanged(enabled)
        if (enabled) {
            onGranted()
        } else {
            onDenied()
        }
    }
    return {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            onStatusChanged(true)
            onGranted()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            settingsLauncher.launch(intent)
        }
    }
}
