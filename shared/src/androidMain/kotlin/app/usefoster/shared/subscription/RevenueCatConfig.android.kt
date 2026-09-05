package app.usefoster.shared.subscription

import app.usefoster.shared.secrets.Secrets

/**
 * Public Google Play SDK key (prefixed `goog_`), injected at startup from
 * BuildConfig — which Gradle fills from env vars / the gitignored
 * local.properties. Never commit real values.
 */
actual val revenueCatApiKey: String
    get() = Secrets.revenueCatApiKey
