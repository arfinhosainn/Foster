package app.usenekko.onboarding.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPermissionController(): PermissionController {
    return remember {
        object : PermissionController {
            override fun requestPermission(
                permission: Permission,
                onResult: (PermissionStatus) -> Unit
            ) {
                when (permission) {
                    Permission.Notification -> requestNotificationPermission(onResult)
                }
            }
        }
    }
}

private fun requestNotificationPermission(onResult: (PermissionStatus) -> Unit) {
    val options = UNAuthorizationOptionAlert or
        UNAuthorizationOptionSound or
        UNAuthorizationOptionBadge

    UNUserNotificationCenter.currentNotificationCenter()
        .requestAuthorizationWithOptions(options) { granted, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                onResult(granted.toPermissionStatus())
            }
        }
}

private fun Boolean.toPermissionStatus(): PermissionStatus {
    return if (this) PermissionStatus.Granted else PermissionStatus.Denied
}
