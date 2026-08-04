package app.usenekko.onboarding.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    const val SUPABASE_URL = "https://ulrzuzrwilemkcahsvih.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVscnp1enJ3aWxlbWtjYWhzdmloIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUxNTQ4MDcsImV4cCI6MjEwMDczMDgwN30.w2bhaXtDugN53rnuaIyXm57vGoVNe7-1qeTT_lwhONc"
    const val GOOGLE_WEB_CLIENT_ID = "874656360216-clvksukjpp8jpmusoo93mv11auiumsdq.apps.googleusercontent.com"
}

fun createAppSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.SUPABASE_URL,
    supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY,
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
    install(ComposeAuth) {
        googleNativeLogin(serverClientId = SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
    }
    install(Functions)
}
