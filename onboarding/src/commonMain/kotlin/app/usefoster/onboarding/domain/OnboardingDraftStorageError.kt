package app.usefoster.onboarding.domain

sealed interface OnboardingDraftStorageError {
    data object Read : OnboardingDraftStorageError
    data object Write : OnboardingDraftStorageError
    data object Clear : OnboardingDraftStorageError
    data object Corrupt : OnboardingDraftStorageError
}

fun OnboardingDraftStorageError.toUserMessage(): String = when (this) {
    OnboardingDraftStorageError.Read -> "We could not restore your setup. You can continue and try again later"
    OnboardingDraftStorageError.Write -> "Your latest progress could not be saved. Please try again"
    OnboardingDraftStorageError.Clear -> "Your setup is complete, but we could not clear its local copy"
    OnboardingDraftStorageError.Corrupt -> "We could not restore your saved setup. You can start again"
}