package app.usefoster.home.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usefoster.home.di.rememberCheckInHistoryViewModel
import app.usefoster.home.presentation.components.CheckInTimelineGrid
import app.usefoster.home.presentation.components.TIMELINE_SLOT_COUNT
import app.usefoster.home.presentation.components.TimelineSlot
import app.usefoster.home.presentation.components.buildTimelineSlots
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.date_mdY
import foster.home.generated.resources.history_board_checkins
import foster.home.generated.resources.history_board_label
import foster.home.generated.resources.history_board_missed
import foster.home.generated.resources.history_boards_filled
import foster.home.generated.resources.history_boards_perfect
import foster.home.generated.resources.history_dot_missed
import foster.home.generated.resources.history_dot_none
import foster.home.generated.resources.history_dot_position
import foster.home.generated.resources.history_empty
import foster.home.generated.resources.history_first_board_progress
import foster.home.generated.resources.history_perfect_badge
import foster.home.generated.resources.month_apr
import foster.home.generated.resources.month_aug
import foster.home.generated.resources.month_dec
import foster.home.generated.resources.month_feb
import foster.home.generated.resources.month_jan
import foster.home.generated.resources.month_jul
import foster.home.generated.resources.month_jun
import foster.home.generated.resources.month_mar
import foster.home.generated.resources.month_may
import foster.home.generated.resources.month_nov
import foster.home.generated.resources.month_oct
import foster.home.generated.resources.month_sep
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Check-in history screen: an archive of finished 26-dot boards.
 *
 * Each section is one completed cycle rendered with the exact same
 * [CheckInTimelineGrid] as Home (bottom-origin 1+7+7+7+4 layout, avatars,
 * check badges, missed gaps). The header carries the board's date range and
 * stats; tapping a dot opens a sheet with the exact date and check-in times.
 *
 * Boards are 26-day windows anchored to the user's first-ever activity, so
 * they always line up with Home's cycle math. The current in-progress board
 * and abandoned (zero-activity) boards are excluded — see [buildBoardUiModels].
 *
 * Data comes from the shared [app.usefoster.home.data.HomeRepository] through
 * [CheckInHistoryViewModel] — the same snapshot Home already loads.
 */
@Composable
fun CheckInHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckInHistoryViewModel = rememberCheckInHistoryViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedDot by viewModel.selectedDot.collectAsStateWithLifecycle()
    // Visual "today" for slot rendering (elapsed/future classification).
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    Scaffold(
        modifier = modifier,
        containerColor = FosterTheme.colors.background.b0,
        topBar = { HistoryTopBar(onBack = onBack) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = FosterTheme.colors.text.primary,
                        strokeWidth = 2.dp,
                    )
                }

                state.boards.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.currentBoardProgress == null) {
                            // No check-ins ever — nothing to archive.
                            stringResource(Res.string.history_empty)
                        } else {
                            // Check-ins exist, but they all live in the current
                            // in-progress board; history shows finished boards.
                            stringResource(
                                Res.string.history_first_board_progress,
                                state.currentBoardProgress!!,
                                TIMELINE_SLOT_COUNT,
                            )
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = FosterTheme.colors.text.tertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))

                    BoardsFilledStatRow(
                        filled = state.boardsFilled,
                        perfect = state.perfectCount,
                    )

                    Spacer(Modifier.height(24.dp))

                    state.boards.forEach { board ->
                        BoardSection(
                            board = board,
                            today = today,
                            onDotClick = viewModel::selectDot,
                        )
                        Spacer(Modifier.height(28.dp))
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    selectedDot?.let { dot ->
        DotDetailsSheet(dot = dot, onDismiss = viewModel::dismissDot)
    }
}

/** "N boards filled · ★ M perfect" summary at the top of the archive. */
@Composable
private fun BoardsFilledStatRow(
    filled: Int,
    perfect: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.history_boards_filled, filled),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = FosterTheme.colors.text.primary,
        )
        if (perfect > 0) {
            Spacer(Modifier.widthIn(min = 12.dp))
            Text(
                text = "★",
                fontSize = 14.sp,
                color = FosterTheme.colors.green.default,
            )
            Spacer(Modifier.widthIn(min = 4.dp))
            Text(
                text = stringResource(Res.string.history_boards_perfect, perfect),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FosterTheme.colors.green.default,
            )
        }
    }
}

@Composable
private fun BoardSection(
    board: HistoryBoardUiModel,
    today: LocalDate,
    onDotClick: (HistoryBoardUiModel, TimelineSlot) -> Unit,
) {
    val slots = remember(board.startDate, board.events, today) {
        buildTimelineSlots(startDate = board.startDate, today = today, events = board.events)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.history_board_label, board.boardIndex),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = FosterTheme.colors.text.primary,
            )
            Spacer(Modifier.widthIn(min = 8.dp))

            if (board.isPerfect) {
                Spacer(Modifier.weight(1f))
                PerfectBadge()
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.history_board_checkins, board.completedCount) +
                " · " + stringResource(Res.string.history_board_missed, board.missedCount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = FosterTheme.colors.text.tertiary,
        )

        Spacer(Modifier.height(20.dp))

        CheckInTimelineGrid(
            slots = slots,
            animateBubble = false,
            onSlotClick = { slot -> onDotClick(board, slot) },
        )
    }
}

/** ★ Perfect pill for boards with zero missed occurrences. */
@Composable
private fun PerfectBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(FosterTheme.colors.green.fill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "★",
            fontSize = 12.sp,
            color = FosterTheme.colors.green.default,
        )
        Spacer(Modifier.widthIn(min = 4.dp))
        Text(
            text = stringResource(Res.string.history_perfect_badge),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = FosterTheme.colors.green.default,
        )
    }
}

/** "Aug 2 – 29" / "Aug 28 – Sep 2" / "Dec 28, 2026 – Jan 2, 2027". */
@Composable
private fun boardDateRange(start: LocalDate, end: LocalDate): String {
    val startMonth = shortMonthName(start.month)
    val endMonth = shortMonthName(end.month)
    return when {
        start.year == end.year && start.month == end.month ->
            "$startMonth ${start.day} – ${end.day}"
        start.year == end.year ->
            "$startMonth ${start.day} – $endMonth ${end.day}"
        else ->
            "$startMonth ${start.day}, ${start.year} – $endMonth ${end.day}, ${end.year}"
    }
}

/** "Aug 14, 2026" via the shared date_mdY pattern. */
@Composable
private fun dotDate(date: LocalDate): String {
    val monthAbbr = shortMonthName(date.month)
    return stringResource(Res.string.date_mdY, monthAbbr, date.day, date.year)
}

private fun shortMonthRes(month: Month): StringResource = when (month) {
    Month.JANUARY -> Res.string.month_jan
    Month.FEBRUARY -> Res.string.month_feb
    Month.MARCH -> Res.string.month_mar
    Month.APRIL -> Res.string.month_apr
    Month.MAY -> Res.string.month_may
    Month.JUNE -> Res.string.month_jun
    Month.JULY -> Res.string.month_jul
    Month.AUGUST -> Res.string.month_aug
    Month.SEPTEMBER -> Res.string.month_sep
    Month.OCTOBER -> Res.string.month_oct
    Month.NOVEMBER -> Res.string.month_nov
    Month.DECEMBER -> Res.string.month_dec
}

@Composable
private fun shortMonthName(month: Month): String = stringResource(shortMonthRes(month))

/** Bottom sheet shown when a board dot is tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DotDetailsSheet(
    dot: DotDetails,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FosterTheme.colors.background.b1,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.history_board_label, dot.boardIndex) +
                    " · " + stringResource(
                    Res.string.history_dot_position,
                    dot.dotNumber,
                    TIMELINE_SLOT_COUNT,
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FosterTheme.colors.text.tertiary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = dotDate(dot.date),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = FosterTheme.colors.text.primary,
            )

            Spacer(Modifier.height(16.dp))

            if (dot.checkIns.isEmpty() && dot.missedContactNames.isEmpty()) {
                Text(
                    text = stringResource(Res.string.history_dot_none),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = FosterTheme.colors.text.tertiary,
                )
            } else {
                dot.checkIns.forEach { entry ->
                    DotDetailsRow(
                        label = entry.contactName,
                        trailing = entry.time,
                        trailingColor = FosterTheme.colors.text.secondary,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                dot.missedContactNames.forEach { name ->
                    DotDetailsRow(
                        label = name,
                        trailing = stringResource(Res.string.history_dot_missed),
                        trailingColor = FosterTheme.colors.red.default,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DotDetailsRow(
    label: String,
    trailing: String?,
    trailingColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = FosterTheme.colors.text.primary,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = trailingColor,
            )
        }
    }
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
