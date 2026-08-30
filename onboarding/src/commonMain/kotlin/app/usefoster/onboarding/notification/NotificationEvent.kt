package app.usefoster.onboarding.notification

import app.usefoster.onboarding.domain.OnboardingProfileError

sealed interface NotificationEvent {
    data object NavigateToMainApp : NotificationEvent
    data class ShowError(val error: OnboardingProfileError) : NotificationEvent
}
