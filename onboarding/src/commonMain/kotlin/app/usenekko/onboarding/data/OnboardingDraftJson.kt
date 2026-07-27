package app.usenekko.onboarding.data

import kotlinx.serialization.json.Json

val onboardingJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
