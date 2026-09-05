package app.usefoster.onboarding.data.supabase

import app.usefoster.shared.secrets.Secrets
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout

object SupabaseConfig {
    // Injected at build time via shared/secrets/Secrets.kt — never commit real
    // values (the repo is public). See local.properties.example (Android) and
    // Secrets.xcconfig.example (iOS).
    val SUPABASE_URL: String
        get() = Secrets.supabaseUrl

    /** Publishable key (sb_publishable_… or the legacy anon JWT pre-migration). */
    val SUPABASE_PUBLISHABLE_KEY: String
        get() = Secrets.supabasePublishableKey

    val GOOGLE_WEB_CLIENT_ID: String
        get() = Secrets.googleWebClientId
}

@OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
fun createAppSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.SUPABASE_URL,
    supabaseKey = SupabaseConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    install(Auth) {
        host = "auth-callback"
        scheme = "app.usefoster"
        autoLoadFromStorage = true
        autoSaveToStorage = true
        alwaysAutoRefresh = true
        enableLifecycleCallbacks = true
    }
    install(Postgrest)
    install(Storage)
    install(ComposeAuth) {
        googleNativeLogin(serverClientId = SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
        appleNativeLogin()
    }
    install(Functions)
    // The brainstorm Edge Function calls an LLM whose response can take a few
    // seconds. Without a generous client timeout, a slow (but successful) call
    // surfaces as a spurious "network error". Set a comfortable ceiling here.
    httpConfig {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
        }
    }
}
