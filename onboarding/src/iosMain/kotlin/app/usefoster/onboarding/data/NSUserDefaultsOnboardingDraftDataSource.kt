package app.usefoster.onboarding.data

import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class NSUserDefaultsOnboardingDraftDataSource(
    private val json: Json = onboardingJson,
) : OnboardingDraftLocalDataSource {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val key = "onboarding_draft_json"

    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> {
        return try {
            val encoded = defaults.stringForKey(key)
            if (encoded == null) return Result.Success(OnboardingDraft())
            try {
                Result.Success(json.decodeFromString<OnboardingDraft>(encoded))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                println("OnboardingDraft[iOS]: corrupt saved draft")
                Result.Error(OnboardingDraftStorageError.Corrupt)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            println("OnboardingDraft[iOS]: read failed")
            Result.Error(OnboardingDraftStorageError.Read)
        }
    }

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> {
        return try {
            defaults.setObject(json.encodeToString(draft), forKey = key)
            if (!defaults.synchronize()) {
                Result.Error(OnboardingDraftStorageError.Write)
            } else {
                Result.Success(Unit)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            println("OnboardingDraft[iOS]: write failed")
            Result.Error(OnboardingDraftStorageError.Write)
        }
    }

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> {
        return try {
            defaults.removeObjectForKey(key)
            if (!defaults.synchronize()) {
                Result.Error(OnboardingDraftStorageError.Clear)
            } else {
                Result.Success(Unit)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            println("OnboardingDraft[iOS]: clear failed")
            Result.Error(OnboardingDraftStorageError.Clear)
        }
    }
}
