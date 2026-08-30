package app.usefoster.onboarding

import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import org.junit.Assert.assertNotNull
import org.junit.Test

class SupabaseAppleAuthConfigurationTest {

    @Test
    fun appleNativeAuthConfigurationEnablesAppleLogin() {
        val config = ComposeAuth.Config().apply {
            appleNativeLogin()
        }

        assertNotNull(config.appleLoginConfig)
    }
}