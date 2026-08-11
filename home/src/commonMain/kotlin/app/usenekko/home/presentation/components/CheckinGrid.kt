package app.usenekko.home.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.usenekko.theme.NekkoTheme
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_circlecheckmark
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

const val TIMELINE_COLUMNS = 7
const val TIMELINE_ROWS = 5

/** Bottom to top: 1 + 7 + 7 + 7 + 4 = 26 chronological positions. */
private val TIMELINE_ROW_CAPACITIES = listOf(1, 7, 7, 7, 4)
private val TIMELINE_ROW_ALIGNMENTS = listOf(
    RowAlignment.Start,
    RowAlignment.Start,
    RowAlignment.Start,
    RowAlignment.Start,
    RowAlignment.End,
)

private enum class RowAlignment { Start, End }

const val TIMELINE_SLOT_COUNT = 26
const val TIMELINE_CURRENT_SLOT_INDEX = 12
const val CHECK_IN_BUBBLE_DURATION_MILLIS = 5_000L
val CHECK_IN_BUBBLE_SIZE = 96.dp
val CHECK_IN_BUBBLE_MIN_SIZE = 86.dp

private const val MAX_VISIBLE_AVATARS = 2
private const val SECOND_AVATAR_VERTICAL_OFFSET_RATIO = 0.20f
private const val BUBBLE_ANIMATION_DURATION_MILLIS = 1000
private val MAX_TIMELINE_CELL_SIZE = 50.dp
private val TIMELINE_AVATAR_SIZE = 40.dp
private val STACKED_AVATAR_SIZE = 32.dp
private val STACKED_INDICATOR_INSET = 1.dp
private const val STACKED_INDICATOR_VERTICAL_LIFT_RATIO = 0.30f
private val MIN_TIMELINE_HORIZONTAL_SPACING = 8.dp
private val MIN_TIMELINE_VERTICAL_SPACING = 9.dp
private val CellShape = RoundedCornerShape(percent = 42)

fun timelineRowSlotIndices(visualRow: Int, rightToLeft: Boolean = true): List<Int> {
    require(visualRow in 0 until TIMELINE_ROWS) {
        "visualRow must be between 0 and ${TIMELINE_ROWS - 1}"
    }

    val rowCapacity = TIMELINE_ROW_CAPACITIES[visualRow]
    val rowStartIndex = TIMELINE_ROW_CAPACITIES.take(visualRow).sum()
    return (0 until rowCapacity).map { column ->
        rowStartIndex + if (rightToLeft) rowCapacity - 1 - column else column
    }
}

fun timelineRowLeadingEmptyColumns(visualRow: Int): Int {
    require(visualRow in 0 until TIMELINE_ROWS) {
        "visualRow must be between 0 and ${TIMELINE_ROWS - 1}"
    }

    val emptyColumns = TIMELINE_COLUMNS - TIMELINE_ROW_CAPACITIES[visualRow]
    val alignment = TIMELINE_ROW_ALIGNMENTS[visualRow]
    return if (alignment == RowAlignment.End) emptyColumns else 0
}

fun timelineCellSizeForWidth(
    maxWidth: Dp,
    horizontalSpacing: Dp = MIN_TIMELINE_HORIZONTAL_SPACING,
): Dp =
    ((maxWidth - horizontalSpacing * (TIMELINE_COLUMNS - 1)) / TIMELINE_COLUMNS)
        .coerceAtMost(MAX_TIMELINE_CELL_SIZE)
        .coerceAtLeast(0.dp)

fun timelineRowSpacing(verticalSpacing: Dp = 6.dp): Dp =
    verticalSpacing.coerceAtLeast(MIN_TIMELINE_VERTICAL_SPACING)

fun timelineAvatarSize(index: Int, visibleCount: Int, cellSize: Dp): Dp {
    val targetSize = if (visibleCount > 1 && index == visibleCount - 1) {
        STACKED_AVATAR_SIZE
    } else {
        TIMELINE_AVATAR_SIZE
    }
    return cellSize.coerceAtMost(targetSize)
}

enum class TimelineAvatarIndicatorAnchor {
    SingleAvatar,
    StackedAvatarCorner,
}

fun timelineStackedAvatarIndicatorOffset(cellSize: Dp): DpOffset {
    val frontAvatarSize = timelineAvatarSize(index = 1, visibleCount = 2, cellSize = cellSize)
    return DpOffset(
        x = -((cellSize - frontAvatarSize) / 2 + STACKED_INDICATOR_INSET),
        y = (cellSize - frontAvatarSize) / 2 +
            avatarStackYOffset(index = 1, visibleCount = 2, cellSize = cellSize) +
            STACKED_INDICATOR_INSET - cellSize * STACKED_INDICATOR_VERTICAL_LIFT_RATIO,
    )
}

fun timelineAvatarIndicatorAnchor(visibleCount: Int): TimelineAvatarIndicatorAnchor =
    if (visibleCount > 1) {
        TimelineAvatarIndicatorAnchor.StackedAvatarCorner
    } else {
        TimelineAvatarIndicatorAnchor.SingleAvatar
    }

@Immutable
data class TimelineSlot(
    val date: LocalDate,
    val isElapsed: Boolean,
    val isCurrent: Boolean,
    val isFuture: Boolean,
    val isCheckedIn: Boolean = false,
    val hasPendingCheckIn: Boolean = false,
    val avatars: List<DrawableResource> = emptyList(),
    val avatarCount: Int = avatars.size,
    val plant: DrawableResource? = null,
    val isSelected: Boolean = false,
)

@Immutable
data class TimelineEvent(
    val date: LocalDate,
    val checkedIn: Boolean = false,
    val avatars: List<DrawableResource> = emptyList(),
    val avatarCount: Int = avatars.size,
    val plant: DrawableResource? = null,
)

fun buildTimelineSlots(
    startDate: LocalDate,
    today: LocalDate,
    events: Collection<TimelineEvent>,
    selectedDate: LocalDate? = null,
): List<TimelineSlot> {
    val eventByDate = events.groupBy { it.date }
    return List(TIMELINE_SLOT_COUNT) { index ->
        val date = startDate.plus(DatePeriod(days = index))
        val dayEvents = eventByDate[date].orEmpty()
        val avatars = dayEvents.flatMap { it.avatars }
        TimelineSlot(
            date = date,
            isElapsed = date < today,
            isCurrent = date == today,
            isFuture = date > today,
            isCheckedIn = dayEvents.isNotEmpty() && dayEvents.all { it.checkedIn },
            hasPendingCheckIn = dayEvents.any { !it.checkedIn },
            avatars = avatars,
            avatarCount = dayEvents.sumOf { maxOf(it.avatarCount, it.avatars.size) },
            plant = dayEvents.firstNotNullOfOrNull { it.plant },
            isSelected = date == selectedDate,
        )
    }
}

fun timelineStartForToday(today: LocalDate): LocalDate =
    today.minus(DatePeriod(days = TIMELINE_CURRENT_SLOT_INDEX))

@Composable
fun rememberTimelineSlots(
    today: LocalDate,
    events: List<TimelineEvent>,
    selectedDate: LocalDate? = null,
): List<TimelineSlot> = remember(today, events, selectedDate) {
    buildTimelineSlots(timelineStartForToday(today), today, events, selectedDate)
}

@Immutable
data class TimelineGridColors(
    val cell: Color,
    val checkedIn: Color,
    val currentOutline: Color,
    val selectedOutline: Color,
    val badge: Color,
    val badgeText: Color,
    val bubble: Color,
)

@Composable
fun timelineGridColors(
    cell: Color = NekkoTheme.colors.fill.secondary,
    checkedIn: Color = Color(0xFF128B43),
    currentOutline: Color = Color(0xFF28D86F),
    selectedOutline: Color = Color(0xFFFFD400),
    badge: Color = Color(0xFFF4F4F6),
    badgeText: Color = Color(0xFF1C1C1F),
    bubble: Color = defaultTimelineBubbleColor(NekkoTheme.colors.fill.secondary),
): TimelineGridColors = TimelineGridColors(
    cell, checkedIn, currentOutline, selectedOutline, badge, badgeText, bubble,
)

fun defaultTimelineBubbleColor(fillSecondary: Color): Color = fillSecondary.copy(alpha = 0.05f)

val avatarRingBrush = Brush.sweepGradient(
    listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
)

fun avatarCellBackground(surfaceColor: Color, baseColor: Color): Color =
    surfaceColor.compositeOver(baseColor)

fun isCheckInBubbleAnimationEnabled(
    appInForeground: Boolean,
    hasPendingToday: Boolean,
    bubbleWindowActive: Boolean,
): Boolean = shouldStartCheckInBubbleWindow(appInForeground, hasPendingToday) && bubbleWindowActive

fun shouldStartCheckInBubbleWindow(
    appInForeground: Boolean,
    hasPendingToday: Boolean,
): Boolean = appInForeground && hasPendingToday

@Composable
fun CheckInTimelineGrid(
    slots: List<TimelineSlot>,
    modifier: Modifier = Modifier,
    colors: TimelineGridColors = timelineGridColors(),
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 6.dp,
    animateBubble: Boolean = true,
    onSlotClick: ((TimelineSlot) -> Unit)? = null,
) {
    require(slots.size == TIMELINE_SLOT_COUNT) {
        "CheckInTimelineGrid requires exactly $TIMELINE_SLOT_COUNT chronological slots."
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val effectiveHorizontalSpacing =
            horizontalSpacing.coerceAtLeast(MIN_TIMELINE_HORIZONTAL_SPACING)
        val effectiveVerticalSpacing = timelineRowSpacing(verticalSpacing)
        val cellSize = timelineCellSizeForWidth(maxWidth, effectiveHorizontalSpacing)
        val gridWidth = cellSize * TIMELINE_COLUMNS +
                effectiveHorizontalSpacing * (TIMELINE_COLUMNS - 1)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(effectiveVerticalSpacing),
        ) {
            for (visualRow in TIMELINE_ROWS - 1 downTo 0) {
                val rowCapacity = TIMELINE_ROW_CAPACITIES[visualRow]
                val emptyColumns = TIMELINE_COLUMNS - rowCapacity
                val leadingEmptyColumns = timelineRowLeadingEmptyColumns(visualRow)

                Row(
                    modifier = Modifier.size(width = gridWidth, height = cellSize),
                    horizontalArrangement = Arrangement.spacedBy(effectiveHorizontalSpacing),
                ) {
                    repeat(leadingEmptyColumns) {
                        Spacer(Modifier.size(cellSize))
                    }
                    timelineRowSlotIndices(visualRow).forEach { slotIndex ->
                        val slot = slots[slotIndex]
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .zIndex(if (slot.avatarCount > 0) 2f else if (slot.isCurrent) 1f else 0f),
                            contentAlignment = Alignment.Center,
                        ) {
                            TimelineCell(slot, colors, animateBubble, onSlotClick, cellSize)
                        }
                    }
                    repeat(emptyColumns - leadingEmptyColumns) {
                        Spacer(Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCell(
    slot: TimelineSlot,
    colors: TimelineGridColors,
    animateBubble: Boolean,
    onClick: ((TimelineSlot) -> Unit)?,
    cellSize: Dp,
) {
    val stateDescription = when {
        slot.isCurrent && slot.hasPendingCheckIn -> "Today, check-in pending"
        slot.isCurrent -> "Today"
        slot.isCheckedIn -> "Checked in on ${slot.date}"
        slot.isFuture -> "Upcoming date ${slot.date}"
        else -> "No check-in on ${slot.date}"
    }

    // Important: this wrapper is intentionally NOT clipped so avatar clusters and badges
    // can paint beyond the cell boundary.
    val hitTarget = Modifier
        .requiredSize(cellSize)
        .semantics { contentDescription = stateDescription }
        .then(
            if (onClick == null) Modifier
            else Modifier.clickable(role = Role.Button) { onClick(slot) }
        )

    Box(contentAlignment = Alignment.Center) {
        Box(modifier = hitTarget, contentAlignment = Alignment.Center) {
            when {
                slot.avatarCount > 0 -> AvatarCell(
                    slot = slot,
                    colors = colors,
                    cellSize = cellSize,
                    showBubble = slot.isCurrent && slot.hasPendingCheckIn && animateBubble,
                    animateBubble = animateBubble,
                )

                slot.isFuture -> FutureDot(slot, colors, cellSize)
                else -> EmptyOrCheckedCell(slot, colors, cellSize)
            }
        }
    }
}

/** A soft bubble that repeatedly shrinks around the front avatar. */
@Composable
private fun BouncingAvatarBubble(
    color: Color,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val bubbleScale = if (animate && !LocalInspectionMode.current) {
        val transition = rememberInfiniteTransition(label = "checkInBubble")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = CHECK_IN_BUBBLE_MIN_SIZE.value / CHECK_IN_BUBBLE_SIZE.value,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = BUBBLE_ANIMATION_DURATION_MILLIS),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "checkInBubbleSize",
        )
        value
    } else 1f

    Box(
        modifier = Modifier
            .then(modifier)
            .requiredSize(CHECK_IN_BUBBLE_SIZE * bubbleScale)
            .background(color, CircleShape),
    )
}

@Composable
private fun EmptyOrCheckedCell(slot: TimelineSlot, colors: TimelineGridColors, cellSize: Dp) {
    Box(
        modifier = Modifier
            .size(cellSize)
            .background(if (slot.isCheckedIn) colors.checkedIn else colors.cell, CellShape)
            .selectionOutline(slot, colors, CellShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            slot.plant != null -> Image(
                painter = painterResource(slot.plant),
                contentDescription = "Plant grown on ${slot.date}",
                modifier = Modifier.size(cellSize * 0.42f),
                contentScale = ContentScale.Fit,
            )

            slot.isCheckedIn -> Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(cellSize * 0.3f),
            )
        }
    }
}

@Composable
private fun FutureDot(slot: TimelineSlot, colors: TimelineGridColors, cellSize: Dp) {
    Box(
        Modifier
            .size(cellSize * 0.36f)
            .background(colors.cell, CircleShape)
            .selectionOutline(slot, colors, CircleShape),
    )
}

@Composable
private fun AvatarCell(
    slot: TimelineSlot,
    colors: TimelineGridColors,
    cellSize: Dp,
    showBubble: Boolean,
    animateBubble: Boolean,
) {
    val visibleAvatars = slot.avatars.take(MAX_VISIBLE_AVATARS)
    val visibleCount = maxOf(visibleAvatars.size, minOf(slot.avatarCount, MAX_VISIBLE_AVATARS))
    val ringWidth = 1.5.dp
    val clusterOffset = cellSize * 0.10f

    Box(
        modifier = Modifier.requiredSize(
            width = if (visibleCount > 1) cellSize + clusterOffset * 2 else cellSize,
            height = cellSize,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (shouldShowAvatarBubble(visibleCount - 1, visibleCount, showBubble)) {
            BouncingAvatarBubble(
                color = colors.bubble,
                animate = animateBubble,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = if (visibleCount > 1) clusterOffset else 0.dp,
                        y = avatarStackYOffset(visibleCount - 1, visibleCount, cellSize),
                    )
                    .zIndex(visibleCount.toFloat()),
            )
        }

        // Back avatar first, front avatar last. zIndex makes this deterministic.
        repeat(visibleCount) { index ->
            val xOffset = when {
                visibleCount == 1 -> 0.dp
                index == 0 -> -clusterOffset
                else -> clusterOffset
            }
            val yOffset = avatarStackYOffset(index, visibleCount, cellSize)
            val avatarSize = timelineAvatarSize(index, visibleCount, cellSize)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = xOffset, y = yOffset)
                    .size(avatarSize)
                    .zIndex(index.toFloat()),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .background(
                            if (index == visibleCount - 1) {
                                avatarCellBackground(colors.cell, NekkoTheme.colors.background.b0)
                            } else {
                                colors.cell
                            },
                            CircleShape
                        )
                        .border(ringWidth, avatarRingBrush, CircleShape)
                        .padding(ringWidth + 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    visibleAvatars.getOrNull(index)?.let { avatar ->
                        Image(
                            painter = painterResource(avatar),
                            contentDescription = null,
                            modifier = Modifier.clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }

        val indicatorAnchor = timelineAvatarIndicatorAnchor(visibleCount)
        if (slot.isCheckedIn) CheckBadge(cellSize, indicatorAnchor)

        val overflow = timelineAvatarOverflowCount(slot, visibleCount)
        if (overflow > 0) {
            val indicatorOffset = if (
                indicatorAnchor == TimelineAvatarIndicatorAnchor.StackedAvatarCorner
            ) {
                timelineStackedAvatarIndicatorOffset(cellSize)
            } else {
                DpOffset(
                    x = if (slot.isCheckedIn) -(cellSize * 0.12f) else cellSize * 0.16f,
                    y = -(cellSize * 0.09f),
                )
            }
            Box(
                modifier = Modifier
                    .align(
                        if (indicatorAnchor == TimelineAvatarIndicatorAnchor.StackedAvatarCorner) {
                            Alignment.TopEnd
                        } else {
                            Alignment.TopCenter
                        }
                    )
                    .offset(
                        x = indicatorOffset.x,
                        y = indicatorOffset.y,
                    )
                    .zIndex(10f)
                    .background(colors.badge, CircleShape)
                    .size(width = 24.dp, height = 19.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$overflow",
                    color = colors.badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

fun timelineAvatarOverflowCount(slot: TimelineSlot, visibleCount: Int): Int =
    if (slot.isCheckedIn) 0 else (slot.avatarCount - visibleCount).coerceAtLeast(0)

fun shouldShowAvatarBubble(index: Int, visibleCount: Int, showBubble: Boolean): Boolean =
    showBubble && visibleCount > 0 && index == visibleCount - 1

fun avatarStackYOffset(index: Int, visibleCount: Int, cellSize: Dp): Dp =
    if (visibleCount > 1 && index == 1) {
        cellSize * SECOND_AVATAR_VERTICAL_OFFSET_RATIO
    } else {
        0.dp
    }

@Composable
private fun BoxScope.CheckBadge(
    cellSize: Dp,
    indicatorAnchor: TimelineAvatarIndicatorAnchor,
) {
    val indicatorOffset = if (
        indicatorAnchor == TimelineAvatarIndicatorAnchor.StackedAvatarCorner
    ) {
        timelineStackedAvatarIndicatorOffset(cellSize)
    } else {
        DpOffset(x = cellSize * 0.05f, y = -(cellSize * 0.06f))
    }
    Icon(
        imageVector = vectorResource(Res.drawable.ic_circlecheckmark),
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(
                x = indicatorOffset.x,
                y = indicatorOffset.y,
            )
            .zIndex(11f)
            .size(16.dp),
    )
}

private fun Modifier.selectionOutline(
    slot: TimelineSlot,
    colors: TimelineGridColors,
    shape: Shape,
): Modifier = when {
    slot.isSelected -> border(1.5.dp, colors.selectedOutline, shape)
    slot.isCurrent -> border(1.5.dp, colors.currentOutline, shape)
    else -> this
}

@Composable
fun CheckInTimelineGridSample(modifier: Modifier = Modifier) {
    val today = LocalDate(2026, 7, 31)
    val events = listOf(
        TimelineEvent(today.minus(DatePeriod(days = 11)), checkedIn = true, avatarCount = 4),
        TimelineEvent(today.minus(DatePeriod(days = 4)), checkedIn = true),
        // This pending event is what enables the bubble on today's slot.
        TimelineEvent(today, checkedIn = false, avatarCount = 1),
        TimelineEvent(today.plus(DatePeriod(days = 7)), avatarCount = 1),
    )
    Column(modifier = modifier) {
        Text(
            "Check In", style = NekkoTheme.typography.heading2, fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.primary
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "One contact waiting for check in",
            color = NekkoTheme.colors.text.tertiary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        CheckInTimelineGrid(
            slots = rememberTimelineSlots(
                today = today,
                events = events,
                selectedDate = today.minus(DatePeriod(days = 11)),
            ),
            animateBubble = true,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
