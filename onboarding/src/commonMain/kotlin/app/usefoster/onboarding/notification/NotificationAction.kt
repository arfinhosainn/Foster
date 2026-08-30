package app.usefoster.onboarding.notification

sealed interface NotificationAction {
    data object TurnOnClicked : NotificationAction
    data class PermissionStateChanged(val enabled: Boolean) : NotificationAction
    data class PermissionResult(val granted: Boolean) : NotificationAction
    data object SkipClicked : NotificationAction
}
