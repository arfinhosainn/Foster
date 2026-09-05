package app.usefoster.shared.subscription

import app.usefoster.shared.secrets.Secrets

/**
 * Public Apple SDK key (prefixed `appl_`), injected at startup from Info.plist
 * — which Xcode fills from the gitignored Secrets.xcconfig. Never commit real
 * values.
 */
actual val revenueCatApiKey: String
    get() = Secrets.revenueCatApiKey
