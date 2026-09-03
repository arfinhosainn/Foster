package app.usefoster.home.presentation.history

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.domain.localDate
import app.usefoster.home.presentation.components.TIMELINE_SLOT_COUNT
import app.usefoster.home.presentation.components.TimelineEvent
import app.usefoster.home.presentation.components.TimelineSlot
import app.usefoster.home.presentation.components.avatarResourceForColor
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** UI state for the check-in history screen — an archive of finished boards. */
data class CheckInHistoryState(
    val isLoading: Boolean = true,
    val boards: List<HistoryBoardUiModel> = emptyList(),
    /**
     * Dots already filled in the current in-progress board, or null when the
     * user has no check-ins at all. Lets the empty state distinguish
     * "never checked in" from "first board still in progress".
     */
    val currentBoardProgress: Int? = null,
) {
    val boardsFilled: Int get() = boards.size
    val perfectCount: Int get() = boards.count { it.isPerfect }
}

/**
 * One archived 26-dot board — a snapshot of what Home looked like when that
 * cycle was lived through. [boardIndex] is 1-based from the user's very first
 * activity, so boards always align with Home's cycle math.
 */
data class HistoryBoardUiModel(
    val boardIndex: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completedCount: Int,
    val missedCount: Int,
    /** Sparse check-in/missed events inside the board window; grids fill the rest. */
    val events: List<TimelineEvent>,
) {
    val isPerfect: Boolean get() = missedCount == 0 && completedCount > 0
}

/**
 * LocalDate-domain lookup over the full check-in history, built ONCE per
 * repository snapshot so board derivation is pure map lookups (no per-day
 * scanning of the raw lists).
 *
 * The Instant → LocalDate conversion for [CheckIn.checkedInAt] happens exactly
 * here, through [CheckIn.localDate] — everything downstream works purely in
 * LocalDate, which structurally rules out off-by-one-day bugs.
 */
data class HistoryLookupMaps(
    val completedByDate: Map<LocalDate, Set<String>>,
    val missedByDate: Map<LocalDate, Set<String>>,
)

fun buildHistoryLookupMaps(
    checkIns: List<CheckIn>,
    missedCheckIns: List<MissedCheckIn>,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): HistoryLookupMaps {
    val completed = HashMap<LocalDate, MutableSet<String>>()
    checkIns.forEach { checkIn ->
        val date = checkIn.localDate(timeZone) ?: return@forEach
        completed.getOrPut(date) { mutableSetOf() }.add(checkIn.contactId)
    }
    val missed = HashMap<LocalDate, MutableSet<String>>()
    missedCheckIns.forEach { missedCheckIn ->
        missed.getOrPut(missedCheckIn.scheduledDate) { mutableSetOf() }.add(missedCheckIn.contactId)
    }
    // A contact who missed a scheduled check-in but eventually checked in that
    // day counts as completed only — a late check-in beats the miss marker.
    val effectiveMissed = HashMap<LocalDate, MutableSet<String>>(missed.size)
    missed.forEach { (date, contactIds) ->
        val completedIds = completed[date]
        effectiveMissed[date] = if (completedIds == null) {
            contactIds
        } else {
            contactIds - completedIds
        }.toMutableSet()
    }
    return HistoryLookupMaps(
        completedByDate = completed,
        missedByDate = effectiveMissed,
    )
}

/** Start/end dates of the 1-based [boardIndex] window anchored at [anchor]. */
fun boardWindowFor(anchor: LocalDate, boardIndex: Int): Pair<LocalDate, LocalDate> {
    val start = anchor.plus(DatePeriod(days = (boardIndex - 1) * TIMELINE_SLOT_COUNT))
    val end = start.plus(DatePeriod(days = TIMELINE_SLOT_COUNT - 1))
    return start to end
}

/**
 * Builds the board archive: consecutive [TIMELINE_SLOT_COUNT]-day windows from
 * the user's first-ever activity, newest first.
 *
 * - The current in-progress board (the window containing [today]) is excluded —
 *   it lives on Home.
 * - Abandoned boards (fully elapsed windows with zero check-ins) are excluded —
 *   history only shows boards the user actually lived in.
 */
fun buildBoardUiModels(
    maps: HistoryLookupMaps,
    contacts: List<Contact>,
    anchor: LocalDate,
    today: LocalDate,
): List<HistoryBoardUiModel> {
    val contactIds = contacts.mapTo(mutableSetOf()) { it.id }
    val contactsById = contacts.associateBy { it.id }
    val daysElapsed = (today.toEpochDays() - anchor.toEpochDays()).toInt()
    if (daysElapsed < 0) return emptyList()
    val currentBoardIndex = daysElapsed / TIMELINE_SLOT_COUNT + 1

    return (currentBoardIndex - 1 downTo 1).mapNotNull { boardIndex ->
        val (start, end) = boardWindowFor(anchor, boardIndex)
        var completedCount = 0
        var missedCount = 0
        val events = mutableListOf<TimelineEvent>()
        var date = start
        while (date <= end) {
            val completedIds = maps.completedByDate[date].orEmpty() intersect contactIds
            val missedIds = maps.missedByDate[date].orEmpty() intersect contactIds
            completedCount += completedIds.size
            missedCount += missedIds.size
            (completedIds + missedIds).forEach { contactId ->
                val contact = contactsById.getValue(contactId)
                events += TimelineEvent(
                    date = date,
                    checkedIn = contactId in completedIds,
                    missed = contactId in missedIds,
                    avatars = avatarResourceForColor(contact.avatarColor)?.let(::listOf).orEmpty(),
                    avatarCount = 1,
                )
            }
            date = date.plus(DatePeriod(days = 1))
        }
        // Abandoned board — no check-in activity in the whole window.
        if (completedCount == 0) return@mapNotNull null

        HistoryBoardUiModel(
            boardIndex = boardIndex,
            startDate = start,
            endDate = end,
            completedCount = completedCount,
            missedCount = missedCount,
            events = events,
        )
    }
}

/**
 * Dots already filled in the current in-progress board (the window containing
 * [today]); null when the user has no check-ins at all. A dot counts as filled
 * when at least one contact checked in that day.
 */
fun currentBoardProgress(
    maps: HistoryLookupMaps,
    contactIds: Set<String>,
    anchor: LocalDate,
    today: LocalDate,
): Int? {
    if (maps.completedByDate.isEmpty()) return null
    val daysElapsed = (today.toEpochDays() - anchor.toEpochDays()).toInt()
    if (daysElapsed < 0) return null
    val currentBoardIndex = daysElapsed / TIMELINE_SLOT_COUNT + 1
    val windowStart = anchor.plus(DatePeriod(days = (currentBoardIndex - 1) * TIMELINE_SLOT_COUNT))
    return maps.completedByDate.entries.count { (date, ids) ->
        date >= windowStart && date <= today && ids.any { it in contactIds }
    }
}

/** One row in the dot-detail sheet. */
data class DotCheckInEntry(
    val contactName: String,
    /** Local "HH:mm" time of the check-in; null when unparseable. */
    val time: String?,
)

/** Everything the dot-detail sheet shows for one tapped board dot. */
data class DotDetails(
    val boardIndex: Int,
    /** 1-based chronological dot position within the board (1..26). */
    val dotNumber: Int,
    val date: LocalDate,
    val checkIns: List<DotCheckInEntry>,
    val missedContactNames: List<String>,
)

/**
 * Details for a tapped dot: exact date, its position in the board, every
 * check-in that day (contact + local time) and every missed occurrence.
 */
fun buildDotDetails(
    checkIns: List<CheckIn>,
    maps: HistoryLookupMaps,
    contacts: List<Contact>,
    board: HistoryBoardUiModel,
    slot: TimelineSlot,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): DotDetails {
    val contactIds = contacts.mapTo(mutableSetOf()) { it.id }
    val contactsById = contacts.associateBy { it.id }
    val dotNumber = (slot.date.toEpochDays() - board.startDate.toEpochDays()).toInt() + 1

    val dayCheckIns = checkIns
        .filter { it.contactId in contactIds && it.localDate(timeZone) == slot.date }
        .sortedBy { it.checkedInAt }
        .map { checkIn ->
            DotCheckInEntry(
                contactName = contactsById.getValue(checkIn.contactId).name,
                time = formatCheckInTime(checkIn.checkedInAt, timeZone),
            )
        }
    val missedNames = maps.missedByDate[slot.date].orEmpty()
        .intersect(contactIds)
        .map { contactsById.getValue(it).name }
        .sorted()

    return DotDetails(
        boardIndex = board.boardIndex,
        dotNumber = dotNumber,
        date = slot.date,
        checkIns = dayCheckIns,
        missedContactNames = missedNames,
    )
}

/** Local "HH:mm" rendering of an ISO check-in timestamp; null when unparseable. */
private fun formatCheckInTime(checkedInAt: String, timeZone: TimeZone): String? =
    runCatching {
        val time = kotlin.time.Instant.parse(checkedInAt)
            .toLocalDateTime(timeZone)
            .time
        buildString {
            append(time.hour.toString().padStart(2, '0'))
            append(':')
            append(time.minute.toString().padStart(2, '0'))
        }
    }.getOrNull()

