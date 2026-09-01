package app.usefoster.home.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.home.presentation.components.CheckInMonthGrid
import app.usefoster.home.presentation.components.TimelineEvent
import app.usefoster.home.presentation.components.TimelineSlot
import app.usefoster.designsystem.avatar.avatarResources
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.history_completed_of
import foster.home.generated.resources.history_filter_checkins
import foster.home.generated.resources.history_filter_completed
import foster.home.generated.resources.history_filter_missed
import foster.home.generated.resources.history_missed_of
import foster.home.generated.resources.history_month_april
import foster.home.generated.resources.history_month_august
import foster.home.generated.resources.history_month_december
import foster.home.generated.resources.history_month_february
import foster.home.generated.resources.history_month_january
import foster.home.generated.resources.history_month_july
import foster.home.generated.resources.history_month_june
import foster.home.generated.resources.history_month_march
import foster.home.generated.resources.history_month_may
import foster.home.generated.resources.history_month_november
import foster.home.generated.resources.history_month_october
import foster.home.generated.resources.history_month_september
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource

/** Which days the history calendar highlights. */
enum class HistoryFilter { Completed, Missed, CheckIns }

/** One month of sample check-in data (placeholder until the real logic lands). */
private data class HistoryMonthData(
    val year: Int,
    val monthNumber: Int,
    val events: List<TimelineEvent>,
)

/**
 * Check-in history screen. Opened by tapping the status summary card on Home.
 *
 * Layout top to bottom: top bar (back / "History" / year dropdown), a centered
 * filter row (Completed · Missed · Check-ins), then one section per month —
 * a header row (`August 14 of 16 completed ─── 88%`) above a calendar grid
 * rendered exactly like the home check-in grid.
 *
 * DESIGN PASS ONLY: currently rendered with generated sample data; the real
 * check-in/missed-check-in aggregation comes in a follow-up pass.
 */
@Composable
fun CheckInHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var selectedYear by rememberSaveable { mutableIntStateOf(today.year) }
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilter.CheckIns) }
    val years = remember(today) { listOf(today.year, today.year - 1, today.year - 2) }
    val months = remember(today, selectedYear) { sampleHistoryMonths(today, selectedYear) }

    Scaffold(
        modifier = modifier,
        containerColor = FosterTheme.colors.background.b0,
        topBar = {
            HistoryTopBar(
                onBack = onBack,
                years = years,
                selectedYear = selectedYear,
                onYearSelected = { selectedYear = it },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                HistoryFilterRow(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                )

                Spacer(Modifier.height(24.dp))

                months.forEach { month ->
                    HistoryMonthSection(
                        year = month.year,
                        monthNumber = month.monthNumber,
                        events = month.events,
                        filter = selectedFilter,
                        today = today,
                    )
                    Spacer(Modifier.height(28.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HistoryFilterRow(
    selectedFilter: HistoryFilter,
    onSelectFilter: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HistoryFilterOption(
            filter = HistoryFilter.Completed,
            label = stringResource(Res.string.history_filter_completed),
            selected = selectedFilter == HistoryFilter.Completed,
            onSelect = onSelectFilter,
        )
        HistoryFilterOption(
            filter = HistoryFilter.Missed,
            label = stringResource(Res.string.history_filter_missed),
            selected = selectedFilter == HistoryFilter.Missed,
            onSelect = onSelectFilter,
        )
        HistoryFilterOption(
            filter = HistoryFilter.CheckIns,
            label = stringResource(Res.string.history_filter_checkins),
            selected = selectedFilter == HistoryFilter.CheckIns,
            onSelect = onSelectFilter,
        )
    }
}

@Composable
private fun HistoryFilterOption(
    filter: HistoryFilter,
    label: String,
    selected: Boolean,
    onSelect: (HistoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable { onSelect(filter) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelect(filter) },
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = FosterTheme.colors.text.primary,
        )
    }
}

@Composable
private fun HistoryMonthSection(
    year: Int,
    monthNumber: Int,
    events: List<TimelineEvent>,
    filter: HistoryFilter,
    today: LocalDate,
) {
    val filteredEvents = remember(events, filter, today) {
        events.filteredFor(filter, today)
    }
    val slots = remember(year, monthNumber, filteredEvents, today) {
        buildMonthSlots(year, monthNumber, today, filteredEvents)
    }

    // Stats always come from the full month (elapsed days only), so switching
    // filters doesn't change the denominator.
    val scheduledCount = events.count { it.date <= today }
    val completedCount = events.count { it.date <= today && it.checkedIn && !it.missed }
    val missedCount = scheduledCount - completedCount
    val (headline, total, labelRes) = when (filter) {
        HistoryFilter.Missed -> Triple(missedCount, scheduledCount, Res.string.history_missed_of)
        HistoryFilter.Completed, HistoryFilter.CheckIns ->
            Triple(completedCount, scheduledCount, Res.string.history_completed_of)
    }
    val percent = if (total == 0) 0 else (headline * 100.0 / total).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = historyMonthName(monthNumber),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = FosterTheme.colors.text.primary,
            )
            Spacer(Modifier.widthIn(min = 8.dp))
            Text(
                text = stringResource(labelRes, headline, total),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FosterTheme.colors.text.tertiary,
                maxLines = 1,
            )
            Spacer(Modifier.widthIn(min = 8.dp))
            HistoryDashedLine(modifier = Modifier.weight(1f))
            Spacer(Modifier.widthIn(min = 8.dp))
            Text(
                text = "$percent%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FosterTheme.colors.text.primary,
            )
        }

        Spacer(Modifier.height(20.dp))

        CheckInMonthGrid(slots = slots)
    }
}

/** Dashed line between the month stats and the percentage. */
@Composable
private fun HistoryDashedLine(modifier: Modifier = Modifier) {
    val color = FosterTheme.colors.stroke.secondary
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset.Zero,
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
        )
    }
}

@Composable
private fun historyMonthName(monthNumber: Int): String = stringResource(
    when (monthNumber) {
        1 -> Res.string.history_month_january
        2 -> Res.string.history_month_february
        3 -> Res.string.history_month_march
        4 -> Res.string.history_month_april
        5 -> Res.string.history_month_may
        6 -> Res.string.history_month_june
        7 -> Res.string.history_month_july
        8 -> Res.string.history_month_august
        9 -> Res.string.history_month_september
        10 -> Res.string.history_month_october
        11 -> Res.string.history_month_november
        else -> Res.string.history_month_december
    },
)

// ---------------------------------------------------------------------------
// Month slot building + filtering (logic pass will replace the sample data)
// ---------------------------------------------------------------------------

/**
 * Mirrors [app.usefoster.home.presentation.components.buildTimelineSlots] but
 * for an arbitrary month: one [TimelineSlot] per day of the month, in
 * chronological order, so [CheckInMonthGrid] renders it exactly like home.
 */
private fun buildMonthSlots(
    year: Int,
    monthNumber: Int,
    today: LocalDate,
    events: Collection<TimelineEvent>,
): List<TimelineSlot> {
    val firstDay = LocalDate(year, monthNumber, 1)
    val daysInMonth = firstDay.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).dayOfMonth
    val eventByDate = events.groupBy { it.date }

    return List(daysInMonth) { index ->
        val date = firstDay.plus(DatePeriod(days = index))
        val dayEvents = eventByDate[date].orEmpty()
        val missedEvents = dayEvents.filter { it.missed || (date < today && !it.checkedIn) }
        val visibleEvents = dayEvents - missedEvents.toSet()
        val avatars = visibleEvents.flatMap { it.avatars }
        TimelineSlot(
            date = date,
            isElapsed = date < today,
            isCurrent = date == today,
            isFuture = date > today,
            isCheckedIn = dayEvents.isNotEmpty() && dayEvents.all { it.checkedIn },
            hasPendingCheckIn = dayEvents.any { date >= today && !it.checkedIn },
            hasMissedCheckIn = missedEvents.isNotEmpty(),
            avatars = avatars,
            avatarCount = visibleEvents.sumOf { maxOf(it.avatarCount, it.avatars.size) },
            plant = dayEvents.firstNotNullOfOrNull { it.plant },
        )
    }
}

/**
 * Narrows the event set per filter so the grid only lights the relevant days:
 * days without events render as the small inactive dot, same as home.
 */
private fun List<TimelineEvent>.filteredFor(
    filter: HistoryFilter,
    today: LocalDate,
): List<TimelineEvent> = when (filter) {
    HistoryFilter.Completed -> filter { it.checkedIn && !it.missed }
    HistoryFilter.Missed -> filter { it.missed || (it.date < today && !it.checkedIn) }
    HistoryFilter.CheckIns -> this
}

// ---------------------------------------------------------------------------
// Sample data (design pass only)
// ---------------------------------------------------------------------------

private fun sampleHistoryMonths(today: LocalDate, year: Int): List<HistoryMonthData> {
    val lastMonth = if (year == today.year) today.monthNumber else 12
    return (lastMonth downTo 1).map { monthNumber ->
        HistoryMonthData(
            year = year,
            monthNumber = monthNumber,
            events = sampleMonthEvents(year, monthNumber, today),
        )
    }
}

/** Deterministic per-day sample: most days checked in, ~1 in 10 missed. */
private fun sampleMonthEvents(
    year: Int,
    monthNumber: Int,
    today: LocalDate,
): List<TimelineEvent> {
    val firstDay = LocalDate(year, monthNumber, 1)
    val lastDay = firstDay.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).dayOfMonth
    val sampleAvatars = avatarResources

    val events = mutableListOf<TimelineEvent>()
    for (day in 1..lastDay) {
        val date = LocalDate(year, monthNumber, day)
        if (date > today) break // future days stay empty → inactive dots

        val seed = (day * 7 + monthNumber * 13) % 10
        val missed = seed == 3
        val avatarCount = if (seed % 5 == 0) 2 else 1
        val avatars = List(avatarCount) { i ->
            sampleAvatars[(day + monthNumber * 2 + i) % sampleAvatars.size]
        }
        events += TimelineEvent(
            date = date,
            checkedIn = !missed,
            missed = false,
            avatars = avatars,
        )
    }
    return events
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@PreviewLightDark
@Composable
private fun PreviewCheckInHistoryScreen() {
    FosterTheme {
        CheckInHistoryScreen(onBack = {})
    }
}
