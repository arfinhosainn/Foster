package app.usefoster.shared.secrets

import kotlin.concurrent.Volatile

/**
 * Build-time injected client credentials.
 *
 * HONEST FRAMING — injection is hygiene, not secrecy: these values ship inside
 * the app binary either way (extractable with `strings`). What this actually
 * buys:
 *  - no secret is ever written into source control (the repo is public),
 *  - rotation is a one-line edit in a gitignored local file,
 *  - per-environment configuration.
 * Real data protection lives in RLS policies and server-side rate limits.
 *
 * Bootstrap contract — platforms call [configure] during startup, BEFORE any
 * Supabase client or RevenueCat init:
 *  - Android: MainActivity.onCreate, from BuildConfig (Gradle injects values
 *    from env vars or the gitignored local.properties; a missing value fails
 *    the Gradle build at configuration time).
 *  - iOS: MainViewController, from Info.plist entries injected by Xcode from
 *    the gitignored iosApp/Configuration/Secrets.xcconfig.
 *
 * Accessors fail fast with an actionable message when bootstrapping was
 * skipped — a clear error beats a blank-key 401 at runtime.
 */
object Secrets {
    @Volatile
    private var config: Config? = null

    val isConfigured: Boolean
        get() = config != null

    fun configure(
        supabaseUrl: String,
        supabasePublishableKey: String,
        googleWebClientId: String,
        revenueCatApiKey: String,
    ) {
        config = Config(
            supabaseUrl = supabaseUrl.trim(),
            supabasePublishableKey = supabasePublishableKey.trim(),
            googleWebClientId = googleWebClientId.trim(),
            revenueCatApiKey = revenueCatApiKey.trim(),
        )
    }

    val supabaseUrl: String
        get() = requireConfig().supabaseUrl

    val supabasePublishableKey: String
        get() = requireConfig().supabasePublishableKey

    val googleWebClientId: String
        get() = requireConfig().googleWebClientId

    /** May be blank (e.g. RevenueCat not set up yet) — callers must tolerate that. */
    val revenueCatApiKey: String
        get() = requireConfig().revenueCatApiKey

    private fun requireConfig(): Config =
        config ?: error(
            "Secrets not configured. Call Secrets.configure() during app startup " +
                "before any Supabase/RevenueCat access (see README → Local secrets setup).",
        )

    private data class Config(
        val supabaseUrl: String,
        val supabasePublishableKey: String,
        val googleWebClientId: String,
        val revenueCatApiKey: String,
    )
}