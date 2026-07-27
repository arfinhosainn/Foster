package app.usenekko.onboarding.domain

interface OnboardingProfileDataSource {
    suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError>
    suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError>
}
