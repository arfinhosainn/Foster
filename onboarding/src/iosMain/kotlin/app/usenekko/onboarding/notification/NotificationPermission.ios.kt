package app.usenekko.onboarding.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.usenekko.shared.notifications.ReminderScheduler
import kotlinx.coroutines.launch

@Composable
actual fun rememberNotificationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    // Request authorization ONLY when the user taps "Turn on Notification" — never
    // on app launch. The system prompt appears here, on this explicit action.
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val granted = ReminderScheduler.requestAuthorization()
            if (granted) onGranted() else onDenied()
        }
    }
}