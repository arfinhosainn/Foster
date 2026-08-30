package app.usefoster.onboarding.presentation

import app.usefoster.onboarding.domain.OnboardingAuthError
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.shared.subscription.SubscriptionError
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.error_auth_network
import foster.onboarding.generated.resources.error_auth_provider
import foster.onboarding.generated.resources.error_draft_clear
import foster.onboarding.generated.resources.error_draft_corrupt
import foster.onboarding.generated.resources.error_draft_read
import foster.onboarding.generated.resources.error_draft_write
import foster.onboarding.generated.resources.error_profile_network
import foster.onboarding.generated.resources.error_profile_not_authenticated
import foster.onboarding.generated.resources.error_profile_not_found
import foster.onboarding.generated.resources.error_profile_server
import foster.onboarding.generated.resources.error_subscription_network
import foster.onboarding.generated.resources.error_subscription_unknown
import foster.onboarding.generated.resources.error_subscription_unavailable
import foster.onboarding.generated.resources.error_unexpected
import org.jetbrains.compose.resources.StringResource

fun OnboardingAuthError.toUserMessageResource(): StringResource = when (this) {
    OnboardingAuthError.Network -> Res.string.error_auth_network
    OnboardingAuthError.Provider -> Res.string.error_auth_provider
    OnboardingAuthError.Unexpected -> Res.string.error_unexpected
}

fun OnboardingProfileError.toUserMessageResource(): StringResource = when (this) {
    OnboardingProfileError.NotAuthenticated -> Res.string.error_profile_not_authenticated
    OnboardingProfileError.ProfileNotFound -> Res.string.error_profile_not_found
    OnboardingProfileError.Network -> Res.string.error_profile_network
    OnboardingProfileError.Server -> Res.string.error_profile_server
    is OnboardingProfileError.Unknown -> Res.string.error_unexpected
}

fun OnboardingDraftStorageError.toUserMessageResource(): StringResource = when (this) {
    OnboardingDraftStorageError.Read -> Res.string.error_draft_read
    OnboardingDraftStorageError.Write -> Res.string.error_draft_write
    OnboardingDraftStorageError.Clear -> Res.string.error_draft_clear
    OnboardingDraftStorageError.Corrupt -> Res.string.error_draft_corrupt
}

fun SubscriptionError.toOnboardingUserMessageResource(): StringResource = when (this) {
    SubscriptionError.NotConfigured -> Res.string.error_subscription_unavailable
    SubscriptionError.Network -> Res.string.error_subscription_network
    is SubscriptionError.Unknown -> Res.string.error_subscription_unknown
}