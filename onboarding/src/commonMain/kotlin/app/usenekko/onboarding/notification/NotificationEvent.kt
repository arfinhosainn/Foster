package app.usenekko.onboarding.notification

sealed interface NotificationEvent {
    data object NavigateToMainApp : NotificationEvent
    data class ShowError(val message: String) : NotificationEvent
}
