package app.usenekko.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingDraftStorageError
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class DataStoreOnboardingDraftDataSource(
    private val dataStore: DataStore<Preferences>,
    private val json: kotlinx.serialization.json.Json = onboardingJson,
) : OnboardingDraftLocalDataSource {

    private object Keys {
        val DraftJson = stringPreferencesKey("onboarding_draft_json")
    }

    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> {
        return try {
            val prefs = dataStore.data.first()
            val encoded = prefs[Keys.DraftJson] ?: return Result.Success(OnboardingDraft())
            try {
                Result.Success(json.decodeFromString<OnboardingDraft>(encoded))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                println("OnboardingDraft[Android]: corrupt saved draft")
                Result.Error(OnboardingDraftStorageError.Corrupt)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            println("OnboardingDraft[Android]: read failed")
            Result.Error(OnboardingDraftStorageError.Read)
        }
    }

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> {
        return try {
            dataStore.edit { prefs ->
                prefs[Keys.DraftJson] = json.encodeToString(draft)
            }
            Result.Success(Unit)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            println("OnboardingDraft[Android]: write failed")
            Result.Error(OnboardingDraftStorageError.Write)
        }
    }

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> {
        return try {
            dataStore.edit { prefs ->
                prefs.remove(Keys.DraftJson)
            }
            Result.Success(Unit)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            println("OnboardingDraft[Android]: clear failed")
            Result.Error(OnboardingDraftStorageError.Clear)
        }
    }
}
