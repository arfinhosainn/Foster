package app.usenekko.navigation

import androidx.compose.runtime.Composable

@Composable
fun NekkoNavHost(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    screenContent(navigator.currentScreen)
}
