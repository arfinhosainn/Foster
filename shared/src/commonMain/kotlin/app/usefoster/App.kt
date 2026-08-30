package app.usefoster

import androidx.compose.runtime.Composable
import app.usefoster.navigation.Navigator
import app.usefoster.navigation.FosterNavHost
import app.usefoster.navigation.Screen
import app.usefoster.theme.LocalThemeStore
import app.usefoster.theme.FosterTheme
import app.usefoster.theme.ThemePreferenceStoreProvider

@Composable
fun App(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    if (LocalThemeStore.current != null) {
        // Already inside a ThemePreferenceStoreProvider (e.g. OnboardingApp's root
        // wrapper): reuse that single store so the bottom bar, overlays and screens
        // all follow the same live in-app theme mode.
        FosterTheme {
            FosterNavHost(navigator, screenContent)
        }
    } else {
        ThemePreferenceStoreProvider {
            FosterTheme {
                FosterNavHost(navigator, screenContent)
            }
        }
    }
}
