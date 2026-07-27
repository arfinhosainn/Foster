package app.usenekko.onboarding.domain

sealed interface OnboardingProfileError {
    data object NotAuthenticated : OnboardingProfileError
    data object Network : OnboardingProfileError
    data object Server : OnboardingProfileError
    data object Unknown : OnboardingProfileError
}
