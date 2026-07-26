package app.usenekko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.usenekko.navigation.Screen
import app.usenekko.navigation.rememberNavigator
import app.usenekko.onboarding.OnboardingApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val navigator = rememberNavigator(startDestination = Screen.Welcome)
            BackHandler(enabled = navigator.canGoBack) {
                navigator.goBack()
            }
            OnboardingApp(navigator)
        }
    }
}
