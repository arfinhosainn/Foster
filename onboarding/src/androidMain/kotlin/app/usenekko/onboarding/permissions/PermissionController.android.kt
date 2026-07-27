package app.usenekko.onboarding.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    var pendingResult by remember { mutableStateOf<((PermissionStatus) -> Unit)?>(null) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingResult?.invoke(granted.toPermissionStatus())
        pendingResult = null
    }

    return remember(context, notificationLauncher) {
        object : PermissionController {
            override fun requestPermission(
                permission: Permission,
                onResult: (PermissionStatus) -> Unit
            ) {
                when (permission) {
                    Permission.Notification -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            onResult(PermissionStatus.Granted)
                            return
                        }

                        val permissionName = Manifest.permission.POST_NOTIFICATIONS
                        val alreadyGranted =
                            context.checkSelfPermission(permissionName) == PackageManager.PERMISSION_GRANTED

                        if (alreadyGranted) {
                            onResult(PermissionStatus.Granted)
                        } else {
                            pendingResult = onResult
                            notificationLauncher.launch(permissionName)
                        }
                    }
                }
            }
        }
    }
}

private fun Boolean.toPermissionStatus(): PermissionStatus {
    return if (this) PermissionStatus.Granted else PermissionStatus.Denied
}
