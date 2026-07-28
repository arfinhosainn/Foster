package app.usenekko.onboarding.notification

sealed interface NotificationAction {
    data object TurnOnClicked : NotificationAction
    data class PermissionResult(val granted: Boolean) : NotificationAction
    data object SkipClicked : NotificationAction
}
