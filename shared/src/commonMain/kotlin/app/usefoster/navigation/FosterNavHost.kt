package app.usefoster.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun FosterNavHost(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    AnimatedContent(
        targetState = navigator.navState,
        contentKey = { it.screen },
        transitionSpec = {
            val style = transitionStyle(initialState, targetState)
            val isBackward = targetState.operation == NavigationOperation.Backward
            val transform = when (style) {
                ScreenTransitionStyle.Horizontal -> horizontalTransform(isBackward)
                ScreenTransitionStyle.Vertical -> verticalTransform(isBackward)
                ScreenTransitionStyle.Reset -> resetTransform()
                ScreenTransitionStyle.None -> ContentTransform(
                    targetContentEnter = fadeIn(tween(0)),
                    initialContentExit = fadeOut(tween(0)),
                )
            }

            ContentTransform(
                targetContentEnter = transform.targetContentEnter,
                initialContentExit = transform.initialContentExit,
                targetContentZIndex = targetState.zIndex.toFloat(),
                sizeTransform = SizeTransform(clip = false),
            )
        },
        label = "screen transition",
    ) { state ->
        screenContent(state.screen)
    }
}

private fun horizontalTransform(isBackward: Boolean): ContentTransform {
    val slideSpec = tween<IntOffset>(
        durationMillis = NavAnimationSpecs.HorizontalDurationMillis,
        easing = FastOutSlowInEasing,
    )
    val fadeSpec = tween<Float>(
        durationMillis = NavAnimationSpecs.HorizontalDurationMillis,
        easing = FastOutSlowInEasing,
    )
    return if (isBackward) {
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = slideSpec,
                initialOffsetX = { -it },
            ) + fadeIn(fadeSpec),
            initialContentExit = slideOutHorizontally(
                animationSpec = slideSpec,
                targetOffsetX = { it },
            ) + fadeOut(fadeSpec),
        )
    } else {
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = slideSpec,
                initialOffsetX = { it },
            ) + fadeIn(fadeSpec),
            initialContentExit = slideOutHorizontally(
                animationSpec = slideSpec,
                targetOffsetX = {
                    -(it * NavAnimationSpecs.ForwardParallaxFraction).roundToInt()
                },
            ) + fadeOut(fadeSpec),
        )
    }
}

private fun verticalTransform(isBackward: Boolean): ContentTransform {
    val slideSpec = tween<IntOffset>(
        durationMillis = NavAnimationSpecs.VerticalDurationMillis,
        easing = FastOutSlowInEasing,
    )
    val fadeSpec = tween<Float>(
        durationMillis = NavAnimationSpecs.VerticalDurationMillis,
        easing = FastOutSlowInEasing,
    )
    return if (isBackward) {
        ContentTransform(
            targetContentEnter = slideInVertically(
                animationSpec = slideSpec,
                initialOffsetY = { -it / 4 },
            ) + fadeIn(fadeSpec),
            initialContentExit = slideOutVertically(
                animationSpec = slideSpec,
                targetOffsetY = { it },
            ) + fadeOut(fadeSpec),
        )
    } else {
        ContentTransform(
            targetContentEnter = slideInVertically(
                animationSpec = slideSpec,
                initialOffsetY = { it },
            ) + fadeIn(fadeSpec),
            initialContentExit = slideOutVertically(
                animationSpec = slideSpec,
                targetOffsetY = { -it / 4 },
            ) + fadeOut(fadeSpec),
        )
    }
}

private fun resetTransform(): ContentTransform {
    val animationSpec = tween<Float>(
        durationMillis = NavAnimationSpecs.ResetDurationMillis,
        easing = FastOutSlowInEasing,
    )
    return ContentTransform(
        targetContentEnter = fadeIn(animationSpec) + scaleIn(
            initialScale = NavAnimationSpecs.ResetInitialScale,
            animationSpec = animationSpec,
        ),
        initialContentExit = fadeOut(animationSpec) + scaleOut(
            targetScale = NavAnimationSpecs.ResetInitialScale,
            animationSpec = animationSpec,
        ),
    )
}
