package app.usenekko

import androidx.compose.runtime.Composable
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.NekkoNavHost
import app.usenekko.navigation.Screen
import app.usenekko.theme.LocalThemeStore
import app.usenekko.theme.NekkoTheme
import app.usenekko.theme.ThemePreferenceStoreProvider

@Composable
fun App(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    if (LocalThemeStore.current != null) {
        // Already inside a ThemePreferenceStoreProvider (e.g. OnboardingApp's root
        // wrapper): reuse that single store so the bottom bar, overlays and screens
        // all follow the same live in-app theme mode.
        NekkoTheme {
            NekkoNavHost(navigator, screenContent)
        }
    } else {
        ThemePreferenceStoreProvider {
            NekkoTheme {
                NekkoNavHost(navigator, screenContent)
            }
        }
    }
}
