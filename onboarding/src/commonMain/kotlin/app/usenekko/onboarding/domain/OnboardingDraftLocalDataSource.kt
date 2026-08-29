package app.usenekko.onboarding.domain

import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result

interface OnboardingDraftLocalDataSource {
    suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError>
    suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError>
    suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError>
}
