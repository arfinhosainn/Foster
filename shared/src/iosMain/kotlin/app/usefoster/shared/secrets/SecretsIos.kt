package app.usefoster.shared.secrets

import platform.Foundation.NSBundle

/**
 * iOS bootstrap: reads the build-injected Info.plist entries (values come from
 * the gitignored iosApp/Configuration/Secrets.xcconfig via $(VAR) substitution)
 * and hands them to [Secrets]. Missing values throw here — at startup, with an
 * actionable message — instead of surfacing as opaque 401s later.
 */
fun configureSecretsFromInfoPlist() {
    fun plist(key: String): String =
        NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""

    val supabaseUrl = plist("SUPABASE_URL")
    val supabasePublishableKey = plist("SUPABASE_PUBLISHABLE_KEY")
    val googleWebClientId = plist("GOOGLE_WEB_CLIENT_ID")

    if (supabaseUrl.isBlank() || supabasePublishableKey.isBlank()) {
        error(
            "Missing SUPABASE_URL / SUPABASE_PUBLISHABLE_KEY in Info.plist. " +
                "Create iosApp/Configuration/Secrets.xcconfig from Secrets.xcconfig.example " +
                "(do NOT commit it), then rebuild.",
        )
    }

    Secrets.configure(
        supabaseUrl = supabaseUrl,
        supabasePublishableKey = supabasePublishableKey,
        googleWebClientId = googleWebClientId,
        // Optional — blank means RevenueCat stays unconfigured (free-tier UX).
        revenueCatApiKey = plist("REVENUECAT_IOS_KEY"),
    )
}