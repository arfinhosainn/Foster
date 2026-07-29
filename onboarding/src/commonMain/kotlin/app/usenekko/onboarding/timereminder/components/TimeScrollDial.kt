package app.usenekko.onboarding.timereminder.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.FlingBehavior
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

private const val TOTAL_MINUTES = 12 * 60
private const val WRAP_COUNT = 3

private val TICK_SLOT_WIDTH = 6.dp
private val DIAL_HEIGHT = 120.dp

@Composable
fun TimeScrollDial(
    totalMinutes: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeInitialMinute = totalMinutes.coerceIn(0, TOTAL_MINUTES - 1)
    val centerOffset = WRAP_COUNT * TOTAL_MINUTES

    // This state is created only once.
    // Do not recreate it every time totalMinutes changes.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = centerOffset + safeInitialMinute,
    )

    val snapBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center,
    )

    val selectedMinute by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

            val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2f) - viewportCenter)
            }

            centeredItem?.index
                ?.mod(TOTAL_MINUTES)
                ?.coerceIn(0, TOTAL_MINUTES - 1)
                ?: safeInitialMinute
        }
    }

    /*
     * This is the only direction of synchronization:
     *
     * LazyRow scroll -> selectedMinute -> ViewModel.
     *
     * There is intentionally no LaunchedEffect(totalMinutes) that scrolls
     * the list back. That effect caused the automatic movement loop.
     */
    LaunchedEffect(Unit) {
        snapshotFlow { selectedMinute }
            .distinctUntilChanged()
            .collect { minute ->
                onValueChange(minute)
            }
    }

    val hour = selectedMinute / 60
    val displayHour = if (hour == 0) 12 else hour
    val minute = selectedMinute % 60

    val selectedTimeText =
        "$displayHour:${minute.toString().padStart(2, '0')}"

    val leftMinute = (selectedMinute - 15).mod(TOTAL_MINUTES)
    val rightMinute = (selectedMinute + 15).mod(TOTAL_MINUTES)

    val leftHour = leftMinute / 60
    val rightHour = rightMinute / 60

    val leftTimeText =
        "${if (leftHour == 0) 12 else leftHour}:${(leftMinute % 60).toString().padStart(2, '0')}"

    val rightTimeText =
        "${if (rightHour == 0) 12 else rightHour}:${(rightMinute % 60).toString().padStart(2, '0')}"

    val tickColor = NekkoTheme.colors.text.quaternary
    val labelColor = NekkoTheme.colors.text.tertiary
    val selectedColor = NekkoTheme.colors.text.primary
    val surfaceColor = NekkoTheme.colors.fill.secondary

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
                    text = selectedTimeText,
                    fontSize = 32.sp,
                    color = selectedColor,
                )

                Spacer(Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    Text(
                        text = leftTimeText,
                        fontSize = 14.sp,
                        color = labelColor,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )

                    Text(
                        text = rightTimeText,
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
                    TickRulerRow(
                        listState = listState,
                        snapBehavior = snapBehavior,
                        tickColor = tickColor,
                        dialHeight = DIAL_HEIGHT,
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DIAL_HEIGHT),
                    ) {
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

                    Canvas(
                        modifier = Modifier
                            .width(3.dp)
                            .height(DIAL_HEIGHT)
                            .align(Alignment.Center),
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

@Composable
private fun TickRulerRow(
    listState: LazyListState,
    snapBehavior: FlingBehavior,
    tickColor: Color,
    dialHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val totalItems = (WRAP_COUNT * 2 + 1) * TOTAL_MINUTES

    LazyRow(
        state = listState,
        flingBehavior = snapBehavior,
        modifier = modifier
            .fillMaxWidth()
            .height(dialHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        items(totalItems) { index ->
            val minuteValue = index.mod(TOTAL_MINUTES)

            val isQuarterHour = minuteValue % 15 == 0
            val isFiveMinutes = minuteValue % 5 == 0

            val tickHeightFraction = when {
                isQuarterHour -> 0.82f
                isFiveMinutes -> 0.52f
                else -> 0.32f
            }

            val tickWidth = when {
                isQuarterHour -> 2.dp
                isFiveMinutes -> 1.5.dp
                else -> 1.dp
            }

            val layoutInfo = listState.layoutInfo
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

            val visibleItem =
                layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

            val distanceFraction = if (visibleItem != null) {
                val itemCenter = visibleItem.offset + visibleItem.size / 2f
                val viewportHalf =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f

                (abs(itemCenter - viewportCenter) / viewportHalf)
                    .coerceIn(0f, 1f)
            } else {
                1f
            }

            val alpha = (1f - distanceFraction * 0.65f)
                .coerceIn(0.08f, 1f)

            Box(
                modifier = Modifier
                    .width(TICK_SLOT_WIDTH)
                    .height(dialHeight)
                    .graphicsLayer {
                        this.alpha = alpha
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(
                    modifier = Modifier
                        .width(tickWidth)
                        .height(dialHeight * tickHeightFraction),
                ) {
                    val lineColor = when {
                        isQuarterHour -> tickColor.copy(alpha = 0.8f)
                        isFiveMinutes -> tickColor.copy(alpha = 0.5f)
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