package app.usenekko

import androidx.compose.ui.window.ComposeUIViewController
import app.usenekko.navigation.Screen
import app.usenekko.navigation.rememberNavigator
import app.usenekko.onboarding.OnboardingApp

fun MainViewController() = ComposeUIViewController {
    val navigator = rememberNavigator(startDestination = Screen.Welcome)
    OnboardingApp(navigator)
}
