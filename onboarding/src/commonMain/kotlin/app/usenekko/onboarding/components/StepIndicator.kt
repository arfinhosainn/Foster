package app.usenekko.onboarding.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import app.usenekko.theme.NekkoTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun StepIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(NekkoTheme.colors.background.b1)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isCurrent = index == currentStep
            StepPill(isCurrent = isCurrent)
        }
    }
}

@Composable
private fun StepPill(isCurrent: Boolean) {
    var fillTarget by remember(isCurrent) { mutableStateOf(if (isCurrent) 0f else 1f) }
    LaunchedEffect(isCurrent) {
        fillTarget = 1f
    }

    val width by animateDpAsState(
        targetValue = if (isCurrent) 18.dp else 5.dp,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "step_width",
    )
    val fillProgress by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "step_fill",
    )

    val inactiveColor = NekkoTheme.colors.fill.secondary
    val activeColor = NekkoTheme.colors.text.primary

    Box(
        modifier = Modifier
            .width(width)
            .height(5.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(inactiveColor),
    ) {
        if (isCurrent) {
            val waveTransition = rememberInfiniteTransition(label = "step_wave")
            val wavePhase by waveTransition.animateFloat(
                initialValue = 0f,
                targetValue = (PI * 2).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "step_wave_phase",
            )

            Canvas(modifier = Modifier.size(width, 5.dp)) {
                val fillWidth = size.width * fillProgress
                val amplitude = size.height * 0.28f
                val centerY = size.height / 2f
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(fillWidth, 0f)

                    val samples = 8
                    for (sample in 0..samples) {
                        val progress = sample / samples.toFloat()
                        val y = centerY + sin((progress * PI * 2f) + wavePhase) * amplitude
                        lineTo(fillWidth - (progress * size.height * 1.6f), y.toFloat())
                    }

                    lineTo(0f, size.height)
                    close()
                }

                drawPath(path = path, color = activeColor)
                drawOval(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(
                        x = (fillWidth - size.width * 0.65f).coerceAtLeast(-size.width),
                        y = -size.height * 0.9f,
                    ),
                    size = Size(size.width * 0.8f, size.height * 1.8f),
                )
            }
        }
    }
}
