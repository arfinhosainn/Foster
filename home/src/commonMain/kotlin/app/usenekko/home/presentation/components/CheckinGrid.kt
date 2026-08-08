package app.usenekko.home.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.usenekko.theme.NekkoTheme
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
const val CHECK_IN_PULSE_DURATION_MILLIS = 5_000L
const val CHECK_IN_PULSE_RING_COUNT = 3

private const val MAX_VISIBLE_AVATARS = 2
private const val SECOND_AVATAR_VERTICAL_OFFSET_RATIO = 0.18f
private const val PULSE_STROKE_WIDTH_RATIO = 0.02f
private const val PULSE_MAX_ALPHA = 0.68f
private const val PULSE_CANVAS_SCALE = 1.7f
private val MAX_TIMELINE_CELL_SIZE = 50.dp
private val MIN_TIMELINE_HORIZONTAL_SPACING = 18.dp
private val MIN_TIMELINE_VERTICAL_SPACING = 8.dp
private val CellShape = RoundedCornerShape(percent = 42)

fun timelineCellSizeForWidth(
    maxWidth: Dp,
    horizontalSpacing: Dp = MIN_TIMELINE_HORIZONTAL_SPACING,
): Dp =
    ((maxWidth - horizontalSpacing * (TIMELINE_COLUMNS - 1)) / TIMELINE_COLUMNS)
        .coerceAtMost(MAX_TIMELINE_CELL_SIZE)
        .coerceAtLeast(0.dp)

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
    val pulse: Color,
)

@Composable
fun timelineGridColors(
    cell: Color = NekkoTheme.colors.fill.secondary,
    checkedIn: Color = Color(0xFF128B43),
    currentOutline: Color = Color(0xFF28D86F),
    selectedOutline: Color = Color(0xFFFFD400),
    badge: Color = Color(0xFFF4F4F6),
    badgeText: Color = Color(0xFF1C1C1F),
    pulse: Color = defaultTimelinePulseColor(currentOutline),
): TimelineGridColors = TimelineGridColors(
    cell, checkedIn, currentOutline, selectedOutline, badge, badgeText, pulse,
)

fun defaultTimelinePulseColor(currentOutline: Color): Color = currentOutline

val avatarRingBrush = Brush.sweepGradient(
    listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
)

fun avatarCellBackground(surfaceColor: Color): Color = surfaceColor.copy(alpha = 1f)

fun isCheckInPulseAnimationEnabled(
    appInForeground: Boolean,
    hasPendingToday: Boolean,
    pulseWindowActive: Boolean,
): Boolean = appInForeground && hasPendingToday && pulseWindowActive

@Composable
fun CheckInTimelineGrid(
    slots: List<TimelineSlot>,
    modifier: Modifier = Modifier,
    colors: TimelineGridColors = timelineGridColors(),
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 6.dp,
    animatePulse: Boolean = true,
    onSlotClick: ((TimelineSlot) -> Unit)? = null,
) {
    require(slots.size == TIMELINE_SLOT_COUNT) {
        "CheckInTimelineGrid requires exactly $TIMELINE_SLOT_COUNT chronological slots."
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val effectiveHorizontalSpacing =
            horizontalSpacing.coerceAtLeast(MIN_TIMELINE_HORIZONTAL_SPACING)
        val effectiveVerticalSpacing = verticalSpacing.coerceAtLeast(MIN_TIMELINE_VERTICAL_SPACING)
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
                val rowStartIndex = TIMELINE_ROW_CAPACITIES.take(visualRow).sum()
                val emptyColumns = TIMELINE_COLUMNS - rowCapacity
                val leadingEmptyColumns =
                    if (TIMELINE_ROW_ALIGNMENTS[visualRow] == RowAlignment.End) emptyColumns else 0

                Row(
                    modifier = Modifier.size(width = gridWidth, height = cellSize),
                    horizontalArrangement = Arrangement.spacedBy(effectiveHorizontalSpacing),
                ) {
                    repeat(leadingEmptyColumns) {
                        Spacer(Modifier.size(cellSize))
                    }
                    repeat(rowCapacity) { column ->
                        val slot = slots[rowStartIndex + column]
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .zIndex(if (slot.avatarCount > 0) 2f else if (slot.isCurrent) 1f else 0f),
                            contentAlignment = Alignment.Center,
                        ) {
                            TimelineCell(slot, colors, animatePulse, onSlotClick, cellSize)
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
    animatePulse: Boolean,
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
                    showPulse = slot.isCurrent && slot.hasPendingCheckIn && animatePulse,
                    animatePulse = animatePulse,
                )
                slot.isFuture -> FutureDot(slot, colors, cellSize)
                else -> EmptyOrCheckedCell(slot, colors, cellSize)
            }
        }
    }
}

fun timelinePulseAlpha(phase: Float): Float {
    val clampedPhase = phase.coerceIn(0f, 1f)
    return PULSE_MAX_ALPHA * (1f - clampedPhase)
}

fun timelinePulseStrokeWidth(cellSize: Dp): Dp =
    (cellSize * PULSE_STROKE_WIDTH_RATIO).coerceAtLeast(1.dp)

/** Thin stroked waves expanding from the edge of the front avatar. */
@Composable
private fun PulsingAvatarRipple(
    color: Color,
    animate: Boolean,
    cellSize: Dp,
    ringCount: Int = CHECK_IN_PULSE_RING_COUNT,
) {
    val progress = if (animate && !LocalInspectionMode.current) {
        val transition = rememberInfiniteTransition(label = "checkInPulse")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
            ),
            label = "checkInPulseProgress",
        )
        value
    } else 0.35f

    Canvas(Modifier.requiredSize(cellSize * PULSE_CANVAS_SCALE)) {
        val avatarRadius = cellSize.toPx() / 2f
        val minRadius = avatarRadius + 1.dp.toPx()
        val maxRadius = size.minDimension / 2f
        val strokeWidth = timelinePulseStrokeWidth(cellSize).toPx()
        // Draw the oldest/largest wave first so the avatar stays visually unobstructed.
        val phases = List(ringCount) { index ->
            (progress + index.toFloat() / ringCount) % 1f
        }.sortedDescending()
        phases.forEach { phase ->
            val radius = minRadius + (maxRadius - minRadius) * phase
            drawCircle(
                color = color,
                radius = radius,
                alpha = timelinePulseAlpha(phase),
                style = Stroke(width = strokeWidth),
            )
        }
    }
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
    showPulse: Boolean,
    animatePulse: Boolean,
) {
    val visibleAvatars = slot.avatars.take(MAX_VISIBLE_AVATARS)
    val visibleCount = maxOf(visibleAvatars.size, minOf(slot.avatarCount, MAX_VISIBLE_AVATARS))
    val ringWidth = 1.5.dp
    val clusterOffset = cellSize * 0.22f

    Box(
        modifier = Modifier.requiredSize(
            width = if (visibleCount > 1) cellSize + clusterOffset * 2 else cellSize,
            height = cellSize,
        ),
        contentAlignment = Alignment.Center,
    ) {
        // Back avatar first, front avatar last. zIndex makes this deterministic.
        repeat(visibleCount) { index ->
            val xOffset = when {
                visibleCount == 1 -> 0.dp
                index == 0 -> -clusterOffset
                else -> clusterOffset
            }
            val yOffset = avatarStackYOffset(index, visibleCount, cellSize)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = xOffset, y = yOffset)
                    .size(cellSize)
                    .zIndex(index.toFloat()),
                contentAlignment = Alignment.Center,
            ) {
                if (shouldShowAvatarPulse(index, visibleCount, showPulse)) {
                    PulsingAvatarRipple(
                        color = colors.pulse,
                        animate = animatePulse,
                        cellSize = cellSize,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .background(avatarCellBackground(NekkoTheme.colors.background.b2), CircleShape)
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

        if (slot.isCheckedIn) CheckBadge(colors, cellSize)

        val overflow = (slot.avatarCount - visibleCount).coerceAtLeast(0)
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        x = if (slot.isCheckedIn) -(cellSize * 0.12f) else cellSize * 0.16f,
                        y = -(cellSize * 0.09f),
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

fun shouldShowAvatarPulse(index: Int, visibleCount: Int, showPulse: Boolean): Boolean =
    showPulse && visibleCount > 0 && index == visibleCount - 1

fun avatarStackYOffset(index: Int, visibleCount: Int, cellSize: Dp): Dp =
    if (visibleCount > 1 && index == 1) {
        cellSize * SECOND_AVATAR_VERTICAL_OFFSET_RATIO
    } else {
        0.dp
    }

@Composable
private fun BoxScope.CheckBadge(colors: TimelineGridColors, cellSize: Dp) {
    val badgeSize = (cellSize * 0.3f).coerceAtLeast(16.dp)
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = cellSize * 0.05f, y = -(cellSize * 0.06f))
            .zIndex(11f)
            .size(badgeSize)
            .background(colors.badge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colors.badgeText,
            modifier = Modifier.size((cellSize * 0.18f).coerceAtLeast(11.dp)),
        )
    }
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
        // This pending event is what enables the pulse on today's slot.
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
            animatePulse = true,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
