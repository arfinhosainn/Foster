package app.usenekko.onboarding.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import app.usenekko.shared.notifications.ReminderScheduler
import kotlinx.coroutines.launch

@Composable
actual fun rememberNotificationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onStatusChanged: (Boolean) -> Unit,
): () -> Unit {
    // Request authorization ONLY when the user taps "Turn on Notification" — never
    // on app launch. The system prompt appears here, on this explicit action.
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        onStatusChanged(ReminderScheduler().isEnabled())
    }
    return {
        scope.launch {
            if (ReminderScheduler().isEnabled()) {
                onStatusChanged(true)
                onGranted()
            } else {
                val granted = ReminderScheduler.requestAuthorization()
                onStatusChanged(granted)
                if (granted) onGranted() else onDenied()
            }
        }
    }
}