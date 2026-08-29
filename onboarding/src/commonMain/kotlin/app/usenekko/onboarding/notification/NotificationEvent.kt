package app.usenekko.onboarding.notification

import app.usenekko.onboarding.domain.OnboardingProfileError

sealed interface NotificationEvent {
    data object NavigateToMainApp : NotificationEvent
    data class ShowError(val error: OnboardingProfileError) : NotificationEvent
}
