package app.usenekko.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreOnboardingDraftDataSource(
    private val dataStore: DataStore<Preferences>,
    private val json: kotlinx.serialization.json.Json = onboardingJson,
) : OnboardingDraftLocalDataSource {

    private object Keys {
        val DraftJson = stringPreferencesKey("onboarding_draft_json")
    }

    override suspend fun getDraft(): OnboardingDraft {
        return dataStore.data
            .map { prefs ->
                val encoded = prefs[Keys.DraftJson]
                if (encoded == null) {
                    OnboardingDraft()
                } else {
                    json.decodeFromString<OnboardingDraft>(encoded)
                }
            }
            .first()
    }

    override suspend fun saveDraft(draft: OnboardingDraft) {
        dataStore.edit { prefs ->
            prefs[Keys.DraftJson] = json.encodeToString(draft)
        }
    }

    override suspend fun clearDraft() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.DraftJson)
        }
    }
}
