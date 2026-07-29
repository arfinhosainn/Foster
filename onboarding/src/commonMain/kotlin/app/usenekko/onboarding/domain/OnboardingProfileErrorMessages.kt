package app.usenekko.onboarding.domain

fun OnboardingProfileError.toUserMessage(): String = when (this) {
    OnboardingProfileError.NotAuthenticated -> "Sign in again to continue"
    OnboardingProfileError.Network -> "Check your connection and try again"
    OnboardingProfileError.Server -> "We could not save your setup. Try again"
    is OnboardingProfileError.Unknown -> "Something went wrong: ${detail ?: "Please try again"}"
}
