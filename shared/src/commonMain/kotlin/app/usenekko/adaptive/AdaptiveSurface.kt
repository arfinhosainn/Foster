package app.usenekko.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

data class AdaptiveSurfacePolicy(
    val maxWidth: Dp,
    val horizontalPadding: Dp,
    val isLandscape: Boolean,
)

fun adaptiveSurfacePolicy(
    width: Dp,
    height: Dp,
    fontScale: Float,
): AdaptiveSurfacePolicy {
    val largeFont = fontScale >= 1.5f
    val widthSizeClass = windowWidthSizeClass(width)
    val maxWidth = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> width
        WindowWidthSizeClass.Medium -> if (largeFont) 680.dp else 640.dp
        WindowWidthSizeClass.Expanded -> if (largeFont) 800.dp else 720.dp
    }

    return AdaptiveSurfacePolicy(
        maxWidth = maxWidth,
        horizontalPadding = if (largeFont) 16.dp else 24.dp,
        isLandscape = width > height,
    )
}

fun retainPaneSelection(
    selectedId: String?,
    availableIds: List<String>,
): String? = selectedId?.takeIf { it in availableIds }

/**
 * Keeps modal content readable on tablets and desktop while leaving phone
 * surfaces full width. The caller still owns scrolling and IME insets.
 */
@Composable
fun AdaptiveSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val policy = adaptiveSurfacePolicy(
            width = maxWidth,
            height = maxHeight,
            fontScale = LocalDensity.current.fontScale,
        )
        Box(
            modifier = Modifier
                .widthIn(max = policy.maxWidth)
                .fillMaxWidth()
                .padding(horizontal = policy.horizontalPadding)
                .align(Alignment.Center),
            content = content,
        )
    }
}