package app.usenekko

import androidx.compose.ui.window.ComposeUIViewController
import app.usenekko.navigation.Screen
import app.usenekko.navigation.rememberNavigator
import app.usenekko.onboarding.OnboardingApp
import app.usenekko.shared.subscription.initRevenueCat

fun MainViewController() = ComposeUIViewController {
    initRevenueCat()
    val navigator = rememberNavigator(startDestination = Screen.Welcome)
    OnboardingApp(navigator)
}
