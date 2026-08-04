package app.usenekko

import androidx.compose.runtime.Composable
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.NekkoNavHost
import app.usenekko.navigation.Screen
import app.usenekko.theme.NekkoTheme
import app.usenekko.theme.ThemePreferenceStoreProvider

@Composable
fun App(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    ThemePreferenceStoreProvider {
        NekkoTheme {
            NekkoNavHost(navigator, screenContent)
        }
    }
}
