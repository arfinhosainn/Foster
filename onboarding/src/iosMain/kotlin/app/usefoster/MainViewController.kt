package app.usefoster

import androidx.compose.ui.window.ComposeUIViewController
import app.usefoster.navigation.Screen
import app.usefoster.navigation.rememberNavigator
import app.usefoster.onboarding.OnboardingApp
import app.usefoster.onboarding.data.supabase.createAppSupabaseClient
import app.usefoster.shared.subscription.initRevenueCat

private val appSupabaseClient by lazy { createAppSupabaseClient() }

fun MainViewController() = ComposeUIViewController {
    initRevenueCat()
    val navigator = rememberNavigator(startDestination = Screen.Splash)
    OnboardingApp(navigator, appSupabaseClient)
}
