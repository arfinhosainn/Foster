package app.usenekko.onboarding.timereminder.components

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Total minutes represented on the dial: 12 hours × 60 minutes = 720 ticks.
 * Each tick = 1 minute. A "page" in the lazy row = 1 minute.
 */
private const val TOTAL_MINUTES = 12 * 60   // 720

/**
 * How many extra copies we pad on each side so the user can scroll "infinitely"
 * in either direction without hitting an edge.
 */
private const val WRAP_COUNT = 3            // 3 × 720 = 2160 items on each side

/** Width of each minute slot in the lazy row. */
private val TICK_SLOT_WIDTH = 6.dp

/** Visible tick area height. */
private val DIAL_HEIGHT = 120.dp

/**
 * A horizontal scrolling ruler / dial that lets the user pick a time with
 * 1-minute precision by scrolling.  The ruler has:
 *
 * - Tall ticks every 15 minutes with a time label above.
 * - Medium ticks every 5 minutes.
 * - Short ticks every 1 minute.
 * - A 3D "barrel" perspective effect — ticks shrink and fade near the edges.
 * - A glowing green center-line indicator.
 * - The currently selected time displayed large above the ruler.
 *
 * @param totalMinutes  Current value expressed as minutes since 12:00 (0–719).
 * @param onValueChange Called with the new total-minutes value when the user scrolls.
 */
@Composable
fun TimeScrollDial(
    totalMinutes: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    // ── Lazy-row state ──────────────────────────────────────────────────
    // We center the logical range by placing `WRAP_COUNT` full copies before
    // the "canonical" copy.  The initial item is therefore:
    //   WRAP_COUNT * TOTAL_MINUTES + totalMinutes
    val centerOffset = WRAP_COUNT * TOTAL_MINUTES
    val initialItem = centerOffset + totalMinutes

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialItem)

    // ── Snap behaviour ──────────────────────────────────────────────────
    val snapBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center,
    )

    // ── Derive selected minute from scroll position ─────────────────────
    val selectedMinute by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val closest = info.visibleItemsInfo.minByOrNull {
                abs((it.offset + it.size / 2f) - viewportCenter)
            }
            if (closest != null) {
                ((closest.index % TOTAL_MINUTES) + TOTAL_MINUTES) % TOTAL_MINUTES
            } else {
                totalMinutes
            }
        }
    }

    // Emit changes back to the parent.
    LaunchedEffect(Unit) {
        snapshotFlow { selectedMinute }
            .distinctUntilChanged()
            .collect { onValueChange(it) }
    }

    // Scroll to the requested value when it changes externally.
    LaunchedEffect(totalMinutes) {
        val current = ((listState.firstVisibleItemIndex % TOTAL_MINUTES) + TOTAL_MINUTES) % TOTAL_MINUTES
        if (current != totalMinutes) {
            val target = centerOffset + totalMinutes
            listState.scrollToItem(target)
        }
    }

    // ── Formatted strings ───────────────────────────────────────────────
    val hour = if (selectedMinute / 60 == 0) 12 else selectedMinute / 60
    val minute = selectedMinute % 60
    val selectedTimeText = "${hour}:${minute.toString().padStart(2, '0')}"

    // Adjacent labels (±15 min)
    val leftMinute = ((selectedMinute - 15) + TOTAL_MINUTES) % TOTAL_MINUTES
    val rightMinute = (selectedMinute + 15) % TOTAL_MINUTES
    val leftHour = if (leftMinute / 60 == 0) 12 else leftMinute / 60
    val rightHour = if (rightMinute / 60 == 0) 12 else rightMinute / 60
    val leftTimeText = "${leftHour}:${(leftMinute % 60).toString().padStart(2, '0')}"
    val rightTimeText = "${rightHour}:${(rightMinute % 60).toString().padStart(2, '0')}"

    // ── Colors ──────────────────────────────────────────────────────────
    val tickColor = NekkoTheme.colors.text.quaternary
    val labelColor = NekkoTheme.colors.text.tertiary
    val selectedColor = NekkoTheme.colors.text.primary
    val surfaceColor = NekkoTheme.colors.fill.secondary
    val backgroundColor = NekkoTheme.colors.background.b0

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Container with rounded corners ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .clip(RoundedCornerShape(24.dp))
                .drawWithContent {
                    // Draw semi-transparent background
                    drawRect(surfaceColor)
                    drawContent()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 24.dp),
            ) {
                // ── Selected time display ───────────────────────────────
                Text(
                    text = selectedTimeText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = selectedColor,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(2.dp))

                // ── Adjacent time labels ────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    Text(
                        text = leftTimeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Text(
                        text = rightTimeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Tick ruler ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DIAL_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    // The scrollable tick row
                    TickRulerRow(
                        listState = listState,
                        snapBehavior = snapBehavior,
                        tickColor = tickColor,
                        dialHeight = DIAL_HEIGHT,
                    )

                    // Edge fade overlays
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DIAL_HEIGHT)
                    ) {
                        // Left fade
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    surfaceColor,
                                    surfaceColor.copy(alpha = 0f),
                                ),
                                startX = 0f,
                                endX = size.width * 0.18f,
                            ),
                        )
                        // Right fade
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    surfaceColor.copy(alpha = 0f),
                                    surfaceColor,
                                ),
                                startX = size.width * 0.82f,
                                endX = size.width,
                            ),
                        )
                    }

                    // ── Center indicator line with green glow ───────────
                    Canvas(
                        modifier = Modifier
                            .width(3.dp)
                            .height(DIAL_HEIGHT)
                            .align(Alignment.Center)
                    ) {
                        val centerX = size.width / 2f
                        // Glow
                        drawLine(
                            color = Color(0xFF4ADE80).copy(alpha = 0.35f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round,
                        )
                        // Core line
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
 * The actual horizontally-scrollable row of tick marks.
 *
 * Ticks fade toward the edges via alpha for a subtle depth cue, but
 * the ruler itself is flat / straight with no barrel curvature.
 */
@Composable
private fun TickRulerRow(
    listState: LazyListState,
    snapBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    tickColor: Color,
    dialHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    LazyRow(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = modifier
            .fillMaxWidth()
            .height(dialHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val totalItems = (2 * WRAP_COUNT + 1) * TOTAL_MINUTES

        items(totalItems) { index ->
            val minuteValue = ((index % TOTAL_MINUTES) + TOTAL_MINUTES) % TOTAL_MINUTES

            // ── Tick geometry ───────────────────────────────────────────
            val isQuarterHour = minuteValue % 15 == 0
            val isFiveMin = minuteValue % 5 == 0

            val tickHeightFraction = when {
                isQuarterHour -> 0.82f
                isFiveMin -> 0.52f
                else -> 0.32f
            }

            val tickWidth = when {
                isQuarterHour -> 2.dp
                isFiveMin -> 1.5.dp
                else -> 1.dp
            }

            // ── Distance from viewport center (0 = center, 1 = edge) ───
            val info = listState.layoutInfo
            val viewportCenter =
                (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val visibleItem =
                info.visibleItemsInfo.firstOrNull { it.index == index }

            val distanceFraction = if (visibleItem != null) {
                val itemCenter = visibleItem.offset + visibleItem.size / 2f
                val viewportHalf =
                    (info.viewportEndOffset - info.viewportStartOffset) / 2f
                (abs(itemCenter - viewportCenter) / viewportHalf).coerceIn(0f, 1f)
            } else {
                1f
            }


            // ── Flat dial transforms ─────────────────────────────────────
            // Alpha: fade toward edges for depth.
            val alphaValue = (1f - distanceFraction * 0.65f).coerceIn(0.08f, 1f)

            Box(
                modifier = Modifier
                    .width(TICK_SLOT_WIDTH)
                    .height(dialHeight)
                    .graphicsLayer {
                        alpha = alphaValue
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(
                    modifier = Modifier
                        .width(tickWidth)
                        .height(dialHeight * tickHeightFraction)
                ) {
                    val lineColor = when {
                        isQuarterHour -> tickColor.copy(alpha = 0.8f)
                        isFiveMin -> tickColor.copy(alpha = 0.5f)
                        else -> tickColor.copy(alpha = 0.3f)
                    }

                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = size.width,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

