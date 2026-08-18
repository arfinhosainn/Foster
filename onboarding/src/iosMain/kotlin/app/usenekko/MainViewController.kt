package app.usenekko

import androidx.compose.ui.window.ComposeUIViewController
import app.usenekko.navigation.Screen
import app.usenekko.navigation.rememberNavigator
import app.usenekko.onboarding.OnboardingApp
import app.usenekko.onboarding.data.supabase.createAppSupabaseClient
import app.usenekko.shared.subscription.initRevenueCat

private val appSupabaseClient by lazy { createAppSupabaseClient() }

fun MainViewController() = ComposeUIViewController {
    initRevenueCat()
    val navigator = rememberNavigator(startDestination = Screen.Splash)
    OnboardingApp(navigator, appSupabaseClient)
}
