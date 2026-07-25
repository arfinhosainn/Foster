package app.usenekko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.contact.ContactScreen
import app.usenekko.onboarding.name.NameScreen
import app.usenekko.onboarding.phone.CodeVerificationScreen
import app.usenekko.onboarding.phone.PhoneScreen
import app.usenekko.onboarding.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val navigator = remember { Navigator(startDestination = Screen.Welcome) }
            App(navigator) { screen ->
                when (screen) {
                    is Screen.Welcome -> WelcomeScreen(
                        onNavigateToPhone = { navigator.navigate(Screen.Phone) },
                    )
                    is Screen.Phone -> PhoneScreen(
                        onNavigateToCodeVerification = { phoneNumber ->
                            navigator.navigate(Screen.CodeVerification(phoneNumber))
                        },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.Contact) },
                    )
                    is Screen.CodeVerification -> CodeVerificationScreen(
                        phoneNumber = screen.phoneNumber,
                        onNavigateToNext = { navigator.navigate(Screen.Name) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.Contact) },
                    )
                    is Screen.Name -> NameScreen(
                        onNavigateToNext = { navigator.navigate(Screen.Contact) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.Contact) },
                    )
                    is Screen.Contact -> ContactScreen(
                        onNavigateToNext = { /* TODO: next screen */ },
                        onBack = { navigator.goBack() },
                        onSkip = { /* TODO: skip to main */ },
                    )
                }
            }
        }
    }
}