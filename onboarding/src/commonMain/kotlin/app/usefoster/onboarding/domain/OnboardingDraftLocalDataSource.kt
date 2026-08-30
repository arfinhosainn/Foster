package app.usefoster.onboarding.domain

import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result

interface OnboardingDraftLocalDataSource {
    suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError>
    suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError>
    suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError>
}
