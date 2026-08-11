package app.usenekko.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

@Composable
fun NekkoNavHost(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    AnimatedContent(
        targetState = navigator.currentScreen,
        transitionSpec = {
            when {
                targetState is Screen.Paywall -> {
                    slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.None
                }

                initialState is Screen.Paywall -> {
                    EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
                }

                else -> EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "screen transition",
    ) { screen ->
        screenContent(screen)
    }
}
