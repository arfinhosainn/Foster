package app.usenekko.designsystem.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a simple glassmorphism effect to a Composable.
 * It uses a semi-transparent background and a subtle border to mimic glass.
 * 
 * @param shape The shape of the glass component.
 * @param backgroundColor The background color of the glass (should be semi-transparent).
 * @param borderColor The border color of the glass (should be semi-transparent).
 * @param borderWidth The thickness of the glass border.
 */
fun Modifier.glass(
    shape: Shape,
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
    borderColor: Color = Color.White.copy(alpha = 0.1f),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderColor, shape)
