package app.usenekko.designsystem

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Horizontal "rim light" brush: brightest on the LEFT and RIGHT edges, fading
 * to transparent through the middle. Used with [Modifier.sideShine] to give
 * solid surfaces the same edge-lit look the old frosted-glass navbar had.
 *
 * [intensity] scales both edge stops (1f = full strength).
 */
fun sideShineBrush(intensity: Float = 1f): Brush {
    val hot = 0.18f * intensity
    val mid = 0.05f * intensity
    return Brush.horizontalGradient(
        0.00f to Color.White.copy(alpha = hot), // hot left edge
        0.30f to Color.White.copy(alpha = mid),
        0.50f to Color.Transparent,             // clear through the middle
        0.70f to Color.White.copy(alpha = mid),
        1.00f to Color.White.copy(alpha = hot), // hot right edge
    )
}

/**
 * Draws a thin [sideShineBrush] border along [shape]. Apply AFTER
 * `clip(shape)` + `background(...)` so the shine paints on top of the fill.
 * [intensity] below 1f softens the rim for larger surfaces.
 */
fun Modifier.sideShine(shape: Shape, width: Dp = 1.dp, intensity: Float = 0.4f): Modifier =
    this.border(width, sideShineBrush(intensity), shape)

/**
 * One-sided variant: hot LEFT edge only, fading out before the middle.
 * Right edge stays completely clean — pairs well with right-aligned art.
 * [intensity] scales the left-edge alphas (1f = full strength), same as [sideShine].
 */
fun Modifier.leftEdgeShine(shape: Shape, width: Dp = 1.dp, intensity: Float = 1f): Modifier =
    this.border(
        width,
        Brush.horizontalGradient(
            0.00f to Color.White.copy(alpha = 0.10f * intensity), // hot left edge
            0.35f to Color.White.copy(alpha = 0.03f * intensity),
            0.60f to Color.Transparent,               // gone past a third in
            1.00f to Color.Transparent,
        ),
        shape,
    )
