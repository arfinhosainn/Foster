package app.usefoster.home

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.presentation.components.TIMELINE_SLOT_COUNT
import app.usefoster.home.presentation.components.buildTimelineSlots
import app.usefoster.home.presentation.history.boardWindowFor
import app.usefoster.home.presentation.history.buildBoardUiModels
import app.usefoster.home.presentation.history.buildDotDetails
import app.usefoster.home.presentation.history.buildHistoryLookupMaps
import app.usefoster.home.presentation.history.currentBoardProgress
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-derivation tests for the board-archive history screen. All Instants are
 * parsed in UTC so the checkedInAt → LocalDate boundary is deterministic.
 */
class CheckInHistoryDerivationTest {

    private val utc = TimeZone.UTC
    private val anchor = LocalDate(2026, 6, 1)
    private val today = LocalDate(2026, 8, 14) // 74 days after anchor → board 3 is current

    private fun contact(id: String = "c1") = Contact(
        id = id,
        name = "C$id",
        avatarColor = "#007AFF",
        checkInFrequency = "daily",
        reminderTime = "12:00:00",
        nextCheckInDate = null,
        lastCheckInDate = null,
        streakCount = 0,
    )

    private fun checkIn(contactId: String, date: LocalDate) = CheckIn(
        id = "$contactId-$date",
        contactId = contactId,
        checkedInAt = "${date}T12:00:00Z",
    )

    private fun completeBoardCheckIns(startDate: LocalDate): List<CheckIn> =
        (0 until TIMELINE_SLOT_COUNT).map { offset ->
            checkIn("c1", startDate.plus(DatePeriod(days = offset)))
        }

    @Test
    fun instantConvertedToLocalDateAtLookupBoundary() {
        val maps = buildHistoryLookupMaps(
            checkIns = listOf(checkIn("c1", LocalDate(2026, 8, 14))),
            missedCheckIns = emptyList(),
            timeZone = utc,
        )
        assertEquals(setOf("c1"), maps.completedByDate[LocalDate(2026, 8, 14)])
        assertTrue(maps.missedByDate.isEmpty())
    }

    @Test
    fun lateCheckInBeatsMissMarker() {
        val date = LocalDate(2026, 8, 10)
        val maps = buildHistoryLookupMaps(
            checkIns = listOf(checkIn("c1", date)),
            missedCheckIns = listOf(MissedCheckIn("m1", "c1", date)),
            timeZone = utc,
        )
        assertTrue(maps.completedByDate[date].orEmpty().contains("c1"))
        assertTrue(maps.missedByDate[date].orEmpty().isEmpty())
    }

    @Test
    fun boardWindowsSliceTimeInto26DayChunksFromAnchor() {
        assertEquals(LocalDate(2026, 6, 1) to LocalDate(2026, 6, 26), boardWindowFor(anchor, 1))
        assertEquals(LocalDate(2026, 6, 27) to LocalDate(2026, 7, 22), boardWindowFor(anchor, 2))
        assertEquals(LocalDate(2026, 7, 23) to LocalDate(2026, 8, 17), boardWindowFor(anchor, 3))
    }

    @Test
    fun currentBoardAndAbandonedBoardsAreExcluded() {
        val maps = buildHistoryLookupMaps(
            checkIns = listOf(
                checkIn("c1", LocalDate(2026, 6, 1)),
                checkIn("c1", LocalDate(2026, 6, 2)),
            ),
            missedCheckIns = emptyList(),
            timeZone = utc,
        )
        val boards = buildBoardUiModels(maps, listOf(contact()), anchor, today)

        // Board 2 fully elapsed with zero check-ins → skipped.
        // Board 3 contains today → current, lives on Home → excluded.
        assertEquals(listOf(1), boards.map { it.boardIndex })
        val board = boards.single()
        assertEquals(LocalDate(2026, 6, 1), board.startDate)
        assertEquals(LocalDate(2026, 6, 26), board.endDate)
        assertEquals(2, board.completedCount)
        // Zero missed occurrences in the window → perfect, even if sparse.
        assertTrue(board.isPerfect)
    }

    @Test
    fun boardWithGapsCountsCompletedAndMissed() {
        val boardStart = LocalDate(2026, 6, 1)
        val checkIns = (0 until TIMELINE_SLOT_COUNT)
            .filter { it != 6 } // miss Jun 7
            .map { offset -> checkIn("c1", boardStart.plus(DatePeriod(days = offset))) }
        val maps = buildHistoryLookupMaps(
            checkIns = checkIns,
            missedCheckIns = listOf(MissedCheckIn("m1", "c1", LocalDate(2026, 6, 7))),
            timeZone = utc,
        )
        val boards = buildBoardUiModels(maps, listOf(contact()), anchor, today)

        val board = boards.single()
        assertEquals(25, board.completedCount)
        assertEquals(1, board.missedCount)
        assertFalse(board.isPerfect)
        // Every day of the board has an event: 25 check-ins + 1 missed gap.
        assertEquals(TIMELINE_SLOT_COUNT, board.events.size)
    }

    @Test
    fun perfectBoardDetected() {
        val maps = buildHistoryLookupMaps(
            checkIns = completeBoardCheckIns(LocalDate(2026, 6, 1)),
            missedCheckIns = emptyList(),
            timeZone = utc,
        )
        val boards = buildBoardUiModels(maps, listOf(contact()), anchor, today)

        val board = boards.single()
        assertEquals(TIMELINE_SLOT_COUNT, board.completedCount)
        assertEquals(0, board.missedCount)
        assertTrue(board.isPerfect)
    }

    @Test
    fun eventsCarryContactAvatarResource() {
        val maps = buildHistoryLookupMaps(
            checkIns = listOf(checkIn("c1", LocalDate(2026, 6, 3))),
            missedCheckIns = emptyList(),
            timeZone = utc,
        )
        val boards = buildBoardUiModels(maps, listOf(contact("c1")), anchor, today)
        val event = boards.single().events.single()
        assertEquals(LocalDate(2026, 6, 3), event.date)
        assertEquals(1, event.avatarCount)
        assertEquals(1, event.avatars.size) // #007AFF maps to a known avatar drawable
        assertTrue(event.checkedIn)
        assertFalse(event.missed)
    }

    @Test
    fun multipleContactsSameDayProduceOneEventEach() {
        val maps = buildHistoryLookupMaps(
            checkIns = listOf(
                checkIn("c1", LocalDate(2026, 6, 3)),
                checkIn("c2", LocalDate(2026, 6, 3)),
            ),
            missedCheckIns = emptyList(),
            timeZone = utc,
        )
        val boards = buildBoardUiModels(
            maps,
            listOf(contact("c1"), contact("c2")),
            anchor,
            today,
        )
        val dayEvents = boards.single().events.filter { it.date == LocalDate(2026, 6, 3) }
        assertEquals(2, dayEvents.size)
        assertTrue(dayEvents.all { it.checkedIn && !it.missed })
    }

    @Test
    fun currentBoardProgressCountsFilledDotsInLiveWindow() {
        // Anchor Jun 1, today Aug 14 → current board 3: Jul 23 – Aug 17.
        val maps = buildHistoryLookupMaps(
            checkIns = listOf(
                checkIn("c1", LocalDate(2026, 6, 2)),   // board 1 → not counted
                checkIn("c1", LocalDate(2026, 7, 25)),  // current board ✓
                checkIn("c1", LocalDate(2026, 7, 26)),  // current board ✓
            ),
            missedCheckIns = emptyList(),
            timeZone = utc,
        )
        val progress = currentBoardProgress(
            maps = maps,
            contactIds = setOf("c1"),
            anchor = anchor,
            today = today,
        )
        assertEquals(2, progress)
    }

    @Test
    fun currentBoardProgressNullWithoutAnyCheckIn() {
        val maps = buildHistoryLookupMaps(emptyList(), emptyList(), utc)
        assertNull(currentBoardProgress(maps, setOf("c1"), anchor, today))
    }

    @Test
    fun dotDetailsExposePositionTimesAndMisses() {
        val checkIns = completeBoardCheckIns(LocalDate(2026, 6, 1))
        val maps = buildHistoryLookupMaps(
            checkIns = checkIns,
            missedCheckIns = listOf(MissedCheckIn("m1", "c2", LocalDate(2026, 6, 3))),
            timeZone = utc,
        )
        val contacts = listOf(contact("c1"), contact("c2"))
        val boards = buildBoardUiModels(maps, contacts, anchor, today)
        val board = boards.single()
        val slots = buildTimelineSlots(startDate = board.startDate, today = today, events = board.events)

        // Dot 3 (Jun 3): one check-in at 12:00 UTC + one missed occurrence.
        val details = buildDotDetails(checkIns, maps, contacts, board, slots[2], utc)
        assertEquals(1, details.boardIndex)
        assertEquals(3, details.dotNumber)
        assertEquals(LocalDate(2026, 6, 3), details.date)
        assertEquals(listOf("Cc1"), details.checkIns.map { it.contactName })
        assertEquals("12:00", details.checkIns.single().time)
        assertEquals(listOf("Cc2"), details.missedContactNames)

        // Empty dot: no check-ins, no misses.
        val emptyDetails = buildDotDetails(
            checkIns = checkIns.take(2),
            maps = buildHistoryLookupMaps(checkIns.take(2), emptyList(), utc),
            contacts = contacts,
            board = board,
            slot = slots[5],
            timeZone = utc,
        )
        assertTrue(emptyDetails.checkIns.isEmpty())
        assertTrue(emptyDetails.missedContactNames.isEmpty())
        assertNull(emptyDetails.checkIns.firstOrNull()?.time)
    }
}
