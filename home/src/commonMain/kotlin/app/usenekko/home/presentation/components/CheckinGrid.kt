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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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

private val CellSize = 44.dp
private val FutureDotSize = 16.dp
private val AvatarClusterOffset = 5.dp
private val PulseDiameter = 144.dp
private const val MAX_VISIBLE_AVATARS = 2

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
    pulse: Color = cell,
): TimelineGridColors = TimelineGridColors(
    cell, checkedIn, currentOutline, selectedOutline, badge, badgeText, pulse,
)

val avatarRingBrush = Brush.sweepGradient(
    listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
)

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        for (visualRow in TIMELINE_ROWS - 1 downTo 0) {
            val rowCapacity = TIMELINE_ROW_CAPACITIES[visualRow]
            val rowStartIndex = TIMELINE_ROW_CAPACITIES.take(visualRow).sum()
            val emptyColumns = TIMELINE_COLUMNS - rowCapacity
            val leadingEmptyColumns =
                if (TIMELINE_ROW_ALIGNMENTS[visualRow] == RowAlignment.End) emptyColumns else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                repeat(leadingEmptyColumns) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
                repeat(rowCapacity) { column ->
                    val slot = slots[rowStartIndex + column]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            // Avatar clusters must paint above adjacent empty cells.
                            .zIndex(if (slot.avatarCount > 0) 2f else if (slot.isCurrent) 1f else 0f),
                        contentAlignment = Alignment.Center,
                    ) {
                        TimelineCell(slot, colors, animatePulse, onSlotClick)
                    }
                }
                repeat(emptyColumns - leadingEmptyColumns) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
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
) {
    val stateDescription = when {
        slot.isCurrent && slot.hasPendingCheckIn -> "Today, check-in pending"
        slot.isCurrent -> "Today"
        slot.isCheckedIn -> "Checked in on ${slot.date}"
        slot.isFuture -> "Upcoming date ${slot.date}"
        else -> "No check-in on ${slot.date}"
    }

    // Important: this wrapper is intentionally NOT clipped. The old .clip(CircleShape)
    // cut off the second avatar and the overflow/check badge at the 44dp cell boundary.
    val hitTarget = Modifier
        .requiredSize(CellSize)
        .semantics { contentDescription = stateDescription }
        .then(
            if (onClick == null) Modifier
            else Modifier.clickable(role = Role.Button) { onClick(slot) }
        )

    Box(contentAlignment = Alignment.Center) {
        if (slot.isCurrent && slot.hasPendingCheckIn) {
            PulsingHalo(color = colors.pulse, animate = animatePulse)
        }
        Box(modifier = hitTarget, contentAlignment = Alignment.Center) {
            when {
                slot.avatarCount > 0 -> AvatarCell(slot, colors)
                slot.isFuture -> FutureDot(slot, colors)
                else -> EmptyOrCheckedCell(slot, colors)
            }
        }
    }
}

/** Three soft, filled waves. A single clock keeps their spacing stable forever. */
@Composable
private fun PulsingHalo(
    color: Color,
    animate: Boolean,
    ringCount: Int = 3,
) {
    val progress = if (animate && !LocalInspectionMode.current) {
        val transition = rememberInfiniteTransition(label = "checkInPulse")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
            ),
            label = "checkInPulseProgress",
        )
        value
    } else 0.35f

    Canvas(Modifier.requiredSize(PulseDiameter)) {
        val minRadius = CellSize.toPx() * 0.58f
        val maxRadius = size.minDimension / 2f
        // Draw largest/oldest first, keeping the center clean for the actual cell.
        val phases = List(ringCount) { index ->
            (progress + index.toFloat() / ringCount) % 1f
        }.sortedDescending()
        phases.forEach { phase ->
            val radius = minRadius + (maxRadius - minRadius) * phase
            drawCircle(
                color = color,
                radius = radius,
                alpha = (0.22f * (1f - phase)).coerceIn(0f, 0.22f),
            )
        }
    }
}

@Composable
private fun EmptyOrCheckedCell(slot: TimelineSlot, colors: TimelineGridColors) {
    Box(
        modifier = Modifier
            .size(CellSize)
            .background(if (slot.isCheckedIn) colors.checkedIn else colors.cell, CircleShape)
            .selectionOutline(slot, colors),
        contentAlignment = Alignment.Center,
    ) {
        when {
            slot.plant != null -> Image(
                painter = painterResource(slot.plant),
                contentDescription = "Plant grown on ${slot.date}",
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit,
            )

            slot.isCheckedIn -> Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FutureDot(slot: TimelineSlot, colors: TimelineGridColors) {
    Box(
        Modifier
            .size(FutureDotSize)
            .background(colors.cell, CircleShape)
            .selectionOutline(slot, colors),
    )
}

@Composable
private fun AvatarCell(slot: TimelineSlot, colors: TimelineGridColors) {
    val visibleAvatars = slot.avatars.take(MAX_VISIBLE_AVATARS)
    val visibleCount = maxOf(visibleAvatars.size, minOf(slot.avatarCount, MAX_VISIBLE_AVATARS))
    val ringWidth = if (slot.isSelected || slot.isCurrent) 2.dp else 1.5.dp

    Box(
        modifier = Modifier.requiredSize(
            width = if (visibleCount > 1) CellSize + AvatarClusterOffset * 2 else CellSize,
            height = CellSize,
        ),
        contentAlignment = Alignment.Center,
    ) {
        // Back avatar first, front avatar last. zIndex makes this deterministic.
        repeat(visibleCount) { index ->
            val xOffset = when {
                visibleCount == 1 -> 0.dp
                index == 0 -> -AvatarClusterOffset
                else -> AvatarClusterOffset
            }
            Box(
                modifier = Modifier
                    .offset(x = xOffset)
                    .size(CellSize)
                    .zIndex(index.toFloat())
                    .background(colors.cell, CircleShape)
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

        if (slot.isCheckedIn) CheckBadge(colors)

        val overflow = (slot.avatarCount - visibleCount).coerceAtLeast(0)
        if (overflow > 0) {
            Text(
                text = "+$overflow",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = if (slot.isCheckedIn) (-5).dp else 7.dp, y = (-8).dp)
                    .zIndex(10f)
                    .background(colors.badge, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                color = colors.badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BoxScope.CheckBadge(colors: TimelineGridColors) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-5).dp)
            .zIndex(11f)
            .size(16.dp)
            .background(colors.badge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colors.badgeText,
            modifier = Modifier.size(11.dp),
        )
    }
}

private fun Modifier.selectionOutline(
    slot: TimelineSlot,
    colors: TimelineGridColors,
): Modifier = when {
    slot.isSelected -> border(1.5.dp, colors.selectedOutline, CircleShape)
    slot.isCurrent -> border(1.5.dp, colors.currentOutline, CircleShape)
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
