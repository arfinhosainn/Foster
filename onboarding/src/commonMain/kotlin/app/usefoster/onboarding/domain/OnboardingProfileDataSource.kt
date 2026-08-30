package app.usefoster.onboarding.domain

import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result

interface OnboardingProfileDataSource {
    suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError>
    suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError>
    suspend fun ensureProfileExists(): EmptyResult<OnboardingProfileError>
}
