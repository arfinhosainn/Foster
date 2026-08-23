package app.usenekko.home.addcontact.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val TOTAL_MINUTES = 12 * 60
private val TICK_SLOT_WIDTH = 6.dp
private val DIAL_HEIGHT = 120.dp
private const val CURVE_BOW_FRACTION = 0.30f

/** Fling coasting stops once velocity drops below this (px/s), then the settle spring takes over. */
private const val FLING_VELOCITY_THRESHOLD = 100f

/** Soft spring used to glide into the centered tick instead of stopping abruptly. */
private const val SETTLE_DAMPING = 0.9f
private const val SETTLE_STIFFNESS = 200f

@Composable
fun TimeScrollDial(
    totalMinutes: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeInitialMinute = totalMinutes.coerceIn(0, TOTAL_MINUTES - 1)
    val density = LocalDensity.current
    val tickSpacingPx = with(density) { TICK_SLOT_WIDTH.toPx() }
    val wrapPeriodPx = TOTAL_MINUTES * tickSpacingPx

    // Single source of truth for the dial position: pixels along an infinite,
    // periodic tick strip. It is backed by snapshot state and only read during
    // the draw phase, so scrolling never triggers recomposition.
    val scrollOffset = remember { Animatable(safeInitialMinute * tickSpacingPx) }
    val scope = rememberCoroutineScope()
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val selectedMinute by remember(tickSpacingPx) {
        derivedStateOf {
            (scrollOffset.value / tickSpacingPx).roundToInt().mod(TOTAL_MINUTES)
        }
    }

    /*
     * This is the only direction of synchronization:
     *
     * Dial scroll -> selectedMinute -> ViewModel.
     *
     * There is intentionally no effect that scrolls back when totalMinutes
     * changes. That caused an automatic movement feedback loop previously.
     */
    LaunchedEffect(scrollOffset, tickSpacingPx) {
        snapshotFlow { scrollOffset.value }
            .map { (it / tickSpacingPx).roundToInt().mod(TOTAL_MINUTES) }
            .distinctUntilChanged()
            .collect { minute -> currentOnValueChange(minute) }
    }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            // Wrap into [0, wrapPeriodPx): the tick pattern is periodic, so
            // shifting by whole periods is visually identical and keeps the
            // float values small and precise.
            scrollOffset.snapTo((scrollOffset.value - delta).mod(wrapPeriodPx))
        }
    }

    fun settle(velocity: Float) {
        scope.launch {
            // Coast with friction until the velocity dies out, then glide
            // softly into the nearest tick instead of stopping abruptly.
            scrollOffset.animateDecay(
                -velocity,
                exponentialDecay(absVelocityThreshold = FLING_VELOCITY_THRESHOLD),
            )
            val nearestTick = (scrollOffset.value / tickSpacingPx).roundToInt()
            scrollOffset.animateTo(
                nearestTick * tickSpacingPx,
                spring(dampingRatio = SETTLE_DAMPING, stiffness = SETTLE_STIFFNESS),
            )
            scrollOffset.snapTo(scrollOffset.value.mod(wrapPeriodPx))
        }
    }

    val hour = selectedMinute / 60
    val minute = selectedMinute % 60
    val displayHour = if (hour == 0) 12 else hour
    val leftMinute = (selectedMinute - 15).mod(TOTAL_MINUTES)
    val rightMinute = (selectedMinute + 15).mod(TOTAL_MINUTES)
    val leftHour = leftMinute / 60
    val rightHour = rightMinute / 60
    val timeText = "$displayHour:${minute.toString().padStart(2, '0')}"
    val leftText =
        "${if (leftHour == 0) 12 else leftHour}:${(leftMinute % 60).toString().padStart(2, '0')}"
    val rightText =
        "${if (rightHour == 0) 12 else rightHour}:${(rightMinute % 60).toString().padStart(2, '0')}"
    val tickColor = NekkoTheme.colors.text.quaternary
    val labelColor = NekkoTheme.colors.text.tertiary
    val surfaceColor = NekkoTheme.colors.fill.secondary
    val indicatorBowPx = with(LocalDensity.current) {
        -(DIAL_HEIGHT * CURVE_BOW_FRACTION * 0.5f).toPx()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .clip(RoundedCornerShape(24.dp))
                .drawWithContent {
                    drawRect(surfaceColor)
                    drawContent()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = timeText,
                    fontSize = 32.sp,
                    color = NekkoTheme.colors.text.primary,
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    Text(
                        text = leftText,
                        fontSize = 14.sp,
                        color = labelColor,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Text(
                        text = rightText,
                        fontSize = 14.sp,
                        color = labelColor,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DIAL_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.img_gradientss),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DIAL_HEIGHT)
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Horizontal,
                                onDragStarted = { scrollOffset.stop() },
                                onDragStopped = { velocity -> settle(velocity) },
                            ),
                    ) {
                        drawTickRuler(
                            offsetPx = scrollOffset.value,
                            spacingPx = tickSpacingPx,
                            tickColor = tickColor,
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .width(3.dp)
                            .height(DIAL_HEIGHT * 0.55f)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { translationY = indicatorBowPx },
                    ) {
                        val centerX = size.width / 2f
                        drawLine(
                            color = Color(0xFF4ADE80).copy(alpha = 0.35f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = Color(0xFF4ADE80),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws the whole tick ruler in a single draw pass. Ticks follow the same
 * styling as before: taller/brighter at quarter hours and five-minute marks,
 * fading out towards the edges and bowing along a parabolic curve.
 */
private fun DrawScope.drawTickRuler(
    offsetPx: Float,
    spacingPx: Float,
    tickColor: Color,
) {
    if (spacingPx <= 0f) return
    val center = size.width / 2f
    val halfWidth = center
    val firstIndex = floor((offsetPx - halfWidth) / spacingPx).toInt()
    val lastIndex = ceil((offsetPx + halfWidth) / spacingPx).toInt()

    for (index in firstIndex..lastIndex) {
        val x = center + (index * spacingPx - offsetPx)
        val distanceFraction = (abs(x - center) / halfWidth).coerceIn(0f, 1f)
        val distanceAlpha = (1f - distanceFraction * 0.65f).coerceIn(0.08f, 1f)
        val translationY = size.height * CURVE_BOW_FRACTION *
            (distanceFraction * distanceFraction - 0.5f)

        val minuteValue = index.mod(TOTAL_MINUTES)
        val isQuarterHour = minuteValue % 15 == 0
        val isFiveMinutes = minuteValue % 5 == 0

        val lineLength = size.height * when {
            isQuarterHour -> 0.55f
            isFiveMinutes -> 0.42f
            else -> 0.32f
        }
        val lineWidth = when {
            isQuarterHour -> 2.dp.toPx()
            isFiveMinutes -> 1.5.dp.toPx()
            else -> 1.dp.toPx()
        }
        val baseAlpha = when {
            isQuarterHour -> 0.8f
            isFiveMinutes -> 0.5f
            else -> 0.3f
        }

        drawLine(
            color = tickColor,
            start = Offset(x, size.height - lineLength + translationY),
            end = Offset(x, size.height + translationY),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round,
            alpha = baseAlpha * distanceAlpha,
        )
    }
}
