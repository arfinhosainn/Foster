package app.usenekko.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded,
}

fun windowWidthSizeClass(width: Dp): WindowWidthSizeClass = when {
    width < 600.dp -> WindowWidthSizeClass.Compact
    width < 840.dp -> WindowWidthSizeClass.Medium
    else -> WindowWidthSizeClass.Expanded
}