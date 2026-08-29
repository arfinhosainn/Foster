package app.usenekko.onboarding.domain

sealed interface OnboardingProfileError {
    data object NotAuthenticated : OnboardingProfileError
    data object ProfileNotFound : OnboardingProfileError
    data object Network : OnboardingProfileError
    data object Server : OnboardingProfileError
    data class Unknown(val detail: String? = null) : OnboardingProfileError
}
