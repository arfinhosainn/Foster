package app.usenekko.onboarding.data

import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class NSUserDefaultsOnboardingDraftDataSource(
    private val json: Json = onboardingJson,
) : OnboardingDraftLocalDataSource {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val key = "onboarding_draft_json"

    override suspend fun getDraft(): OnboardingDraft {
        val encoded = defaults.stringForKey(key)
        if (encoded == null) return OnboardingDraft()
        return try {
            json.decodeFromString<OnboardingDraft>(encoded)
        } catch (_: Exception) {
            clearDraft()
            OnboardingDraft()
        }
    }

    override suspend fun saveDraft(draft: OnboardingDraft) {
        defaults.setObject(json.encodeToString(draft), forKey = key)
        defaults.synchronize()
    }

    override suspend fun clearDraft() {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }
}
