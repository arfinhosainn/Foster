package app.usenekko

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.usenekko.navigation.Screen
import app.usenekko.navigation.rememberNavigator
import app.usenekko.onboarding.OnboardingApp
import app.usenekko.onboarding.data.supabase.createAppSupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import app.usenekko.shared.notifications.ReminderScheduler
import app.usenekko.shared.subscription.initRevenueCat

class MainActivity : ComponentActivity() {
    private val supabaseClient by lazy { createAppSupabaseClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        supabaseClient.handleDeeplinks(intent)

        ReminderScheduler.init(applicationContext)
        initRevenueCat()

        setContent {
            val navigator = rememberNavigator(startDestination = Screen.Welcome)
            BackHandler(enabled = navigator.canGoBack) {
                navigator.goBack()
            }
            OnboardingApp(navigator, supabaseClient)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        supabaseClient.handleDeeplinks(intent)
    }
}
