package app.usenekko.onboarding.permissions

enum class Permission {
    Notification
}

enum class PermissionStatus {
    Granted,
    Denied
}

interface PermissionController {
    fun requestPermission(
        permission: Permission,
        onResult: (PermissionStatus) -> Unit
    )
}
