package app.usenekko.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_DURATION_MILLIS
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_MIN_SIZE
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_SIZE
import app.usenekko.home.presentation.components.avatarCellBackground
import app.usenekko.home.presentation.components.avatarStackYOffset
import app.usenekko.home.presentation.components.buildCheckInTimelineEvents
import app.usenekko.home.presentation.components.buildTimelineSlots
import app.usenekko.home.presentation.components.defaultTimelineBubbleColor
import app.usenekko.home.presentation.components.isCheckInBubbleAnimationEnabled
import app.usenekko.home.presentation.components.shouldShowAvatarBubble
import app.usenekko.home.presentation.components.shouldStartCheckInBubbleWindow
import app.usenekko.home.presentation.components.TimelineEvent
import app.usenekko.home.presentation.components.TimelineAvatarIndicatorAnchor
import app.usenekko.home.presentation.components.timelineAvatarIndicatorAnchor
import app.usenekko.home.presentation.components.timelineAvatarSize
import app.usenekko.home.presentation.components.timelineCellSizeForWidth
import app.usenekko.home.presentation.components.timelineRowLeadingEmptyColumns
import app.usenekko.home.presentation.components.timelineRowSpacing
import app.usenekko.home.presentation.components.timelineRowSlotIndices
import app.usenekko.home.presentation.components.timelineStackedAvatarIndicatorOffset
import app.usenekko.home.presentation.components.timelineAvatarOverflowCount
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
    fun secondAvatarIsOffsetTwentyPercentDownWhileTheFirstAvatarStaysCentered() {
        assertEquals(0.dp, avatarStackYOffset(index = 0, visibleCount = 2, cellSize = 100.dp))
        assertEquals(20.dp, avatarStackYOffset(index = 1, visibleCount = 2, cellSize = 100.dp))
    }

    @Test
    fun stackedAvatarUsesSmallerSizeWhileSingleAvatarKeepsFortyDpSize() {
        assertEquals(40.dp, timelineAvatarSize(index = 0, visibleCount = 1, cellSize = 50.dp))
        assertEquals(40.dp, timelineAvatarSize(index = 0, visibleCount = 2, cellSize = 50.dp))
        assertEquals(32.dp, timelineAvatarSize(index = 1, visibleCount = 2, cellSize = 50.dp))
        assertEquals(28.dp, timelineAvatarSize(index = 1, visibleCount = 2, cellSize = 28.dp))
    }

    @Test
    fun stackedAvatarIndicatorsUseTheStackCornerWhileSingleIndicatorsKeepTheirPlacement() {
        assertEquals(
            TimelineAvatarIndicatorAnchor.SingleAvatar,
            timelineAvatarIndicatorAnchor(visibleCount = 1),
        )
        assertEquals(
            TimelineAvatarIndicatorAnchor.StackedAvatarCorner,
            timelineAvatarIndicatorAnchor(visibleCount = 2),
        )
    }

    @Test
    fun stackedAvatarIndicatorIsInsetIntoTheFrontAvatarCorner() {
        val offset = timelineStackedAvatarIndicatorOffset(50.dp)
        assertEquals(-10.dp, offset.x)
        assertEquals(5f, offset.y.value, 0.001f)
    }

    @Test
    fun calendarUsesFiftyDpCellsAtDesignWidth() {
        assertEquals(50.dp, timelineCellSizeForWidth(maxWidth = 500.dp, horizontalSpacing = 8.dp))
        assertEquals(44.57143.dp, timelineCellSizeForWidth(maxWidth = 360.dp, horizontalSpacing = 8.dp))
    }

    @Test
    fun calendarUsesLargerCellsWithTheReducedDefaultGap() {
        assertEquals(42.dp, timelineCellSizeForWidth(maxWidth = 342.dp))
    }

    @Test
    fun calendarRowsFlowChronologicallyFromRightToLeft() {
        assertEquals(listOf(25, 24, 23, 22), timelineRowSlotIndices(visualRow = 4))
        assertEquals(listOf(21, 20, 19, 18, 17, 16, 15), timelineRowSlotIndices(visualRow = 3))
    }

    @Test
    fun calendarIncompleteRowsKeepTheirOriginalAlignment() {
        assertEquals(3, timelineRowLeadingEmptyColumns(visualRow = 4))
        assertEquals(0, timelineRowLeadingEmptyColumns(visualRow = 0))
    }

    @Test
    fun calendarUsesNineDpMinimumRowGap() {
        assertEquals(9.dp, timelineRowSpacing())
        assertEquals(9.dp, timelineRowSpacing(8.dp))
        assertEquals(10.dp, timelineRowSpacing(10.dp))
    }

    @Test
    fun avatarCellBackgroundCompositesSecondaryFillOverTheCalendarBackground() {
        val secondaryFill = Color.Black.copy(alpha = 0.08f)
        val calendarBackground = Color.White

        assertEquals(Color(0xFFEBEBEB), avatarCellBackground(secondaryFill, calendarBackground))
    }

    @Test
    fun checkInBubbleOnlyAnimatesInForegroundWhenTodayHasPendingCheckIn() {
        assertEquals(5_000L, CHECK_IN_BUBBLE_DURATION_MILLIS)
        assertTrue(
            isCheckInBubbleAnimationEnabled(
                appInForeground = true,
                hasPendingToday = true,
                bubbleWindowActive = true,
            ),
        )
        assertTrue(
            !isCheckInBubbleAnimationEnabled(
                appInForeground = true,
                hasPendingToday = true,
                bubbleWindowActive = false,
            ),
        )
        assertTrue(
            !isCheckInBubbleAnimationEnabled(
                appInForeground = false,
                hasPendingToday = true,
                bubbleWindowActive = true,
            ),
        )
        assertTrue(
            !isCheckInBubbleAnimationEnabled(
                appInForeground = true,
                hasPendingToday = false,
                bubbleWindowActive = true,
            ),
        )
    }

    @Test
    fun checkInBubbleWindowStartsWhenPendingTodayLoadsWhileForeground() {
        assertTrue(shouldStartCheckInBubbleWindow(appInForeground = true, hasPendingToday = true))
        assertTrue(!shouldStartCheckInBubbleWindow(appInForeground = true, hasPendingToday = false))
        assertTrue(!shouldStartCheckInBubbleWindow(appInForeground = false, hasPendingToday = true))
    }

    @Test
    fun defaultBubbleColorUsesSecondaryFillAtFivePercentAlpha() {
        val secondaryFill = Color(0xFF18181B).copy(alpha = 0.08f)

        assertEquals(
            secondaryFill.copy(alpha = 0.05f),
            defaultTimelineBubbleColor(secondaryFill),
        )
    }

    @Test
    fun avatarBubbleTargetsOnlyTheFrontAvatar() {
        assertTrue(shouldShowAvatarBubble(index = 0, visibleCount = 1, showBubble = true))
        assertTrue(!shouldShowAvatarBubble(index = 0, visibleCount = 2, showBubble = true))
        assertTrue(shouldShowAvatarBubble(index = 1, visibleCount = 2, showBubble = true))
        assertTrue(!shouldShowAvatarBubble(index = 1, visibleCount = 2, showBubble = false))
    }

    @Test
    fun avatarBubbleUsesRequestedSizeAndWindow() {
        assertEquals(96.dp, CHECK_IN_BUBBLE_SIZE)
        assertEquals(86.dp, CHECK_IN_BUBBLE_MIN_SIZE)
        assertEquals(5_000L, CHECK_IN_BUBBLE_DURATION_MILLIS)
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