package app.usenekko.onboarding.domain

import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result

interface OnboardingProfileDataSource {
    suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError>
    suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError>
    suspend fun ensureProfileExists(): EmptyResult<OnboardingProfileError>
}
