package app.usenekko.onboarding.notification

sealed interface NotificationAction {
    data object TurnOnClicked : NotificationAction
}
