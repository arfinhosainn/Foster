package app.usenekko.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.presentation.components.CHECK_IN_PULSE_DURATION_MILLIS
import app.usenekko.home.presentation.components.CHECK_IN_PULSE_RING_COUNT
import app.usenekko.home.presentation.components.avatarCellBackground
import app.usenekko.home.presentation.components.avatarStackYOffset
import app.usenekko.home.presentation.components.buildCheckInTimelineEvents
import app.usenekko.home.presentation.components.buildTimelineSlots
import app.usenekko.home.presentation.components.defaultTimelinePulseColor
import app.usenekko.home.presentation.components.isCheckInPulseAnimationEnabled
import app.usenekko.home.presentation.components.shouldShowAvatarPulse
import app.usenekko.home.presentation.components.TimelineEvent
import app.usenekko.home.presentation.components.timelineCellSizeForWidth
import app.usenekko.home.presentation.components.timelineAvatarOverflowCount
import app.usenekko.home.presentation.components.timelinePulseAlpha
import app.usenekko.home.presentation.components.timelinePulseStrokeWidth
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEventsTest {
    private val today = LocalDate(2026, 8, 8)

    @Test
    fun checkedInEventsCarryContactAvatarsAndKeepTheFullCount() {
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(
                checkIn("c1", "2026-08-07T12:00:00Z"),
                checkIn("c2", "2026-08-07T13:00:00Z"),
            ),
            contacts = listOf(
                contact("c1", "#007AFF"),
                contact("c2", "#123456"),
            ),
            today = today,
        )

        val checkedIn = events.single { it.date == LocalDate(2026, 8, 7) }
        assertTrue(checkedIn.checkedIn)
        assertEquals(2, checkedIn.avatarCount)
    }

    @Test
    fun completedStackShowsOnlyCheckmarkWhilePendingStackKeepsOverflowBadge() {
        val completedSlot = buildTimelineSlots(
            startDate = today,
            today = today,
            events = listOf(TimelineEvent(today, checkedIn = true, avatarCount = 11)),
        ).single { it.date == today }
        val pendingSlot = buildTimelineSlots(
            startDate = today,
            today = today,
            events = listOf(TimelineEvent(today, checkedIn = false, avatarCount = 11)),
        ).single { it.date == today }

        assertTrue(completedSlot.isCheckedIn)
        assertEquals(0, timelineAvatarOverflowCount(completedSlot, visibleCount = 2))
        assertEquals(9, timelineAvatarOverflowCount(pendingSlot, visibleCount = 2))
    }

    @Test
    fun pendingEventUsesContactsDueTodayAndKeepsUnknownAvatarCount() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("due", "#007AFF", nextCheckInDate = "2026-08-08"),
                contact("future", "#FF3B30", nextCheckInDate = "2026-08-09"),
                contact("unscheduled", "#123456", nextCheckInDate = null),
            ),
            today = today,
        )

        val pending = events.single()
        assertEquals(today, pending.date)
        assertEquals(2, pending.avatarCount)
        assertTrue(!pending.checkedIn)
    }

    @Test
    fun secondAvatarIsOffsetDownWhileTheFirstAvatarStaysCentered() {
        assertEquals(0.dp, avatarStackYOffset(index = 0, visibleCount = 2, cellSize = 100.dp))
        assertEquals(18.dp, avatarStackYOffset(index = 1, visibleCount = 2, cellSize = 100.dp))
    }

    @Test
    fun calendarUsesFiftyDpCellsAtDesignWidth() {
        assertEquals(50.dp, timelineCellSizeForWidth(maxWidth = 500.dp, horizontalSpacing = 18.dp))
        assertEquals(36.dp, timelineCellSizeForWidth(maxWidth = 360.dp, horizontalSpacing = 18.dp))
    }

    @Test
    fun avatarCellBackgroundIsOpaqueToCoverTheAvatarBehindIt() {
        assertEquals(1f, avatarCellBackground(Color.Black.copy(alpha = 0.08f)).alpha)
    }

    @Test
    fun checkInPulseOnlyAnimatesInForegroundWhenTodayHasPendingCheckIn() {
        assertEquals(5_000L, CHECK_IN_PULSE_DURATION_MILLIS)
        assertTrue(
            isCheckInPulseAnimationEnabled(
                appInForeground = true,
                hasPendingToday = true,
                pulseWindowActive = true,
            ),
        )
        assertTrue(
            !isCheckInPulseAnimationEnabled(
                appInForeground = true,
                hasPendingToday = true,
                pulseWindowActive = false,
            ),
        )
        assertTrue(
            !isCheckInPulseAnimationEnabled(
                appInForeground = false,
                hasPendingToday = true,
                pulseWindowActive = true,
            ),
        )
        assertTrue(
            !isCheckInPulseAnimationEnabled(
                appInForeground = true,
                hasPendingToday = false,
                pulseWindowActive = true,
            ),
        )
    }

    @Test
    fun defaultPulseColorIsOpaqueEnoughToBeVisible() {
        val currentOutline = Color(0xFF28D86F)

        assertEquals(currentOutline, defaultTimelinePulseColor(currentOutline))
    }

    @Test
    fun avatarPulseTargetsOnlyTheFrontAvatar() {
        assertTrue(shouldShowAvatarPulse(index = 0, visibleCount = 1, showPulse = true))
        assertTrue(!shouldShowAvatarPulse(index = 0, visibleCount = 2, showPulse = true))
        assertTrue(shouldShowAvatarPulse(index = 1, visibleCount = 2, showPulse = true))
        assertTrue(!shouldShowAvatarPulse(index = 1, visibleCount = 2, showPulse = false))
    }

    @Test
    fun avatarPulseUsesThinFadingRingParameters() {
        assertEquals(3, CHECK_IN_PULSE_RING_COUNT)
        assertTrue(timelinePulseAlpha(phase = 0f) > timelinePulseAlpha(phase = 0.5f))
        assertEquals(0f, timelinePulseAlpha(phase = 1f))
        assertEquals(1.dp, timelinePulseStrokeWidth(cellSize = 50.dp))
    }

    private fun checkIn(contactId: String, checkedInAt: String) = CheckIn(
        id = "check-in-$contactId-$checkedInAt",
        contactId = contactId,
        checkedInAt = checkedInAt,
    )

    private fun contact(
        id: String,
        avatarColor: String?,
        nextCheckInDate: String? = "2026-08-09",
    ) = Contact(
        id = id,
        name = id,
        avatarColor = avatarColor,
        checkInFrequency = "weekly",
        reminderTime = null,
        nextCheckInDate = nextCheckInDate,
        lastCheckInDate = null,
        streakCount = 0,
    )
}