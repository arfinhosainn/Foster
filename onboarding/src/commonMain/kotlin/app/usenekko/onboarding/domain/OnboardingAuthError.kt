package app.usenekko.onboarding.domain

import kotlinx.coroutines.CancellationException

sealed interface OnboardingAuthError {
    data object Network : OnboardingAuthError
    data object Provider : OnboardingAuthError
    data object Unexpected : OnboardingAuthError
}

fun OnboardingAuthError.toUserMessage(): String = when (this) {
    OnboardingAuthError.Network -> "Check your connection and try again"
    OnboardingAuthError.Provider -> "Sign-in did not complete. Try again"
    OnboardingAuthError.Unexpected -> "Something went wrong. Please try again"
}

fun Throwable.toOnboardingAuthError(): OnboardingAuthError? {
    if (this is CancellationException) return null

    val message = message.orEmpty().lowercase()
    return when {
        listOf(
            "network",
            "timeout",
            "timed out",
            "connection",
            "unreachable",
            "offline",
            "internet",
        ).any(message::contains) -> OnboardingAuthError.Network

        listOf(
            "oauth",
            "provider",
            "google",
            "apple",
            "sign in",
            "signin",
            "credential",
            "token",
            "redirect",
        ).any(message::contains) -> OnboardingAuthError.Provider

        else -> OnboardingAuthError.Unexpected
    }
}