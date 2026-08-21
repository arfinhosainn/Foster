package app.usenekko.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.MissedCheckIn
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_DURATION_MILLIS
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_MIN_SIZE
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_SIZE
import app.usenekko.home.presentation.components.TIMELINE_SLOT_COUNT
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
import app.usenekko.home.presentation.components.timelineBubbleSize
import app.usenekko.home.presentation.components.timelineMaxCellSizeForWidth
import app.usenekko.home.presentation.components.timelineRowLeadingEmptyColumns
import app.usenekko.home.presentation.components.timelineRowSpacing
import app.usenekko.home.presentation.components.timelineRowSlotIndices
import app.usenekko.home.presentation.components.timelineStackedAvatarIndicatorOffset
import app.usenekko.home.presentation.components.timelineAvatarOverflowCount
import app.usenekko.home.presentation.components.timelineStartForToday
import app.usenekko.home.presentation.components.shouldRenderTimelineAvatars
import app.usenekko.home.presentation.components.shouldRenderInactiveDayDot
import app.usenekko.home.presentation.components.updateTimelineDate
import app.usenekko.home.presentation.components.resolveInitialCountdownStartDate
import app.usenekko.home.presentation.homeLoadingTimelineRowSlotCounts
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEventsTest {
    private val today = LocalDate(2026, 8, 8)

    @Test
    fun checkedInEventsCarryContactAvatarsAndKeepTheFullCount() {
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(
                checkIn("c1", "2026-08-08T12:00:00Z"),
                checkIn("c2", "2026-08-08T13:00:00Z"),
            ),
            contacts = listOf(
                contact("c1", "#007AFF", frequency = "daily", nextCheckInDate = today.toString()),
                contact("c2", "#123456", frequency = "daily", nextCheckInDate = today.toString()),
            ),
            today = today,
        )

        val checkedIn = events.filter { it.date == today }
        assertEquals(2, checkedIn.size)
        assertTrue(checkedIn.all { it.checkedIn })
        assertEquals(2, checkedIn.sumOf { it.avatarCount })
    }

    @Test
    fun sameContactIsNotStackedTwiceWhenBackendRowsShareCalendarDate() {
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(
                checkIn("c1", "2026-08-26T12:00:00Z"),
                checkIn("c1", "2026-08-26T13:00:00Z"),
            ),
            contacts = listOf(contact("c1", "#007AFF", nextCheckInDate = "2026-08-26")),
            today = LocalDate(2026, 8, 26),
        )

        val event = events.single { it.date == LocalDate(2026, 8, 26) }

        assertEquals(1, event.avatarCount)
    }

    @Test
    fun repeatedDailyCheckInsKeepEachHistoricalDateIndependent() {
        val firstDate = LocalDate(2026, 8, 19)
        val secondDate = LocalDate(2026, 8, 20)
        val currentDate = LocalDate(2026, 8, 21)
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(
                checkIn("first", "${firstDate}T12:00:00Z"),
                checkIn("first", "${secondDate}T12:00:00Z"),
                checkIn("first", "${currentDate}T12:00:00Z"),
                checkIn("second", "${currentDate}T13:00:00Z"),
            ),
            contacts = listOf(
                contact(
                    id = "first",
                    avatarColor = "#007AFF",
                    frequency = "daily",
                    nextCheckInDate = "2026-08-22",
                ).copy(lastCheckInDate = currentDate.toString()),
                contact(
                    id = "second",
                    avatarColor = "#FF3B30",
                    frequency = "daily",
                    nextCheckInDate = "2026-08-22",
                ).copy(lastCheckInDate = currentDate.toString()),
            ),
            today = currentDate,
        )

        val eventsByDate = events.groupBy { it.date }

        assertEquals(1, eventsByDate[firstDate]?.size)
        assertEquals(1, eventsByDate[secondDate]?.size)
        assertEquals(2, eventsByDate[currentDate]?.size)
        assertTrue(eventsByDate.values.flatten().all { it.checkedIn })
    }

    @Test
    fun missedOccurrenceSurvivesAfterTheContactAdvancesToTheNextCheckIn() {
        val missedDate = today.minus(DatePeriod(days = 1))
        val beforeCheckIn = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("c1", "#007AFF", frequency = "daily", nextCheckInDate = missedDate.toString()),
            ),
            today = today,
        )
        val afterCheckIn = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("c1", "${today}T12:00:00Z")),
            contacts = listOf(
                contact("c1", "#007AFF", frequency = "daily", nextCheckInDate = today.plus(DatePeriod(days = 1)).toString()),
            ),
            today = today,
            missedCheckIns = listOf(
                MissedCheckIn("missed-c1-$missedDate", "c1", missedDate),
            ),
        )

        assertTrue(beforeCheckIn.any { it.date == missedDate && !it.checkedIn })
        assertTrue(afterCheckIn.any { it.date == missedDate && !it.checkedIn })
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

        val pending = events.filter { it.date == today }
        assertEquals(2, pending.size)
        assertTrue(pending.all { it.date == today && !it.checkedIn })
        assertEquals(2, pending.sumOf { it.avatarCount })
        assertTrue(events.none { it.date == LocalDate(2026, 8, 9) })
    }

    @Test
    fun firstContactDueTodayOccupiesTheFirstTimelineCell() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("first", "#007AFF", frequency = "daily", nextCheckInDate = today.toString()),
            ),
            today = today,
        )

        val startDate = timelineStartForToday(today, initialCountdownStartDate = today)
        val slots = buildTimelineSlots(
            startDate = startDate,
            today = today,
            events = events,
        )

        assertEquals(today, startDate)
        assertEquals(today, slots.first().date)
        assertEquals(1, slots.first().avatarCount)
        assertTrue(slots.first().isCurrent)
    }

    @Test
    fun initialCountdownKeepsCompletedFirstOccurrenceAtTheStartAsTheDateAdvances() {
        val nextDay = today.plus(DatePeriod(days = 1))
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("first", "${today}T12:00:00Z")),
            contacts = listOf(
                contact("first", "#007AFF", frequency = "daily", nextCheckInDate = nextDay.toString()),
            ),
            today = nextDay,
        )

        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(nextDay, initialCountdownStartDate = today),
            today = nextDay,
            events = events,
        )

        assertEquals(today, slots.first().date)
        assertTrue(slots.first().isCheckedIn)
        assertEquals(nextDay, slots[1].date)
        assertEquals(1, slots[1].avatarCount)
        assertTrue(slots[1].hasPendingCheckIn)
    }

    @Test
    fun initialCountdownAnchorSurvivesTheFirstCheckInRefresh() {
        val nextDay = today.plus(DatePeriod(days = 1))
        val firstContact = contact(
            id = "first",
            avatarColor = "#007AFF",
            frequency = "daily",
            nextCheckInDate = today.toString(),
        )
        val anchorBeforeCheckIn = resolveInitialCountdownStartDate(
            existingStartDate = null,
            checkIns = emptyList(),
            contacts = listOf(firstContact),
            today = today,
        )

        val anchorAfterCheckIn = resolveInitialCountdownStartDate(
            existingStartDate = anchorBeforeCheckIn,
            checkIns = listOf(checkIn("first", "${today}T12:00:00Z")),
            contacts = listOf(
                firstContact.copy(
                    nextCheckInDate = nextDay.toString(),
                    lastCheckInDate = today.toString(),
                    streakCount = 1,
                ),
            ),
            today = nextDay,
        )

        assertEquals(today, anchorBeforeCheckIn)
        assertEquals(today, anchorAfterCheckIn)
        assertEquals(today, timelineStartForToday(nextDay, anchorAfterCheckIn))
    }

    @Test
    fun timelineRestoresTheFirstCheckInAsItsCycleAnchorAfterRestart() {
        val firstCheckInDate = LocalDate(2026, 8, 1)
        val currentDate = firstCheckInDate.plus(DatePeriod(days = 13))
        val restoredAnchor = resolveInitialCountdownStartDate(
            existingStartDate = null,
            checkIns = listOf(checkIn("first", "${firstCheckInDate}T12:00:00Z")),
            contacts = listOf(
                contact("first", "#007AFF", nextCheckInDate = currentDate.plus(DatePeriod(days = 1)).toString()),
            ),
            today = currentDate,
        )

        assertEquals(firstCheckInDate, restoredAnchor)
        assertEquals(firstCheckInDate, timelineStartForToday(currentDate, restoredAnchor))
    }

    @Test
    fun timelineRestoresAnAnchorFromMissedOccurrencesBeforeTheFirstCheckIn() {
        val firstMissedDate = LocalDate(2026, 8, 1)
        val currentDate = firstMissedDate.plus(DatePeriod(days = 5))
        val restoredAnchor = resolveInitialCountdownStartDate(
            existingStartDate = null,
            checkIns = listOf(checkIn("first", "${currentDate}T12:00:00Z")),
            contacts = listOf(
                contact("first", "#007AFF", nextCheckInDate = currentDate.plus(DatePeriod(days = 1)).toString()),
            ),
            today = currentDate,
            missedCheckIns = listOf(
                MissedCheckIn("missed-first-$firstMissedDate", "first", firstMissedDate),
            ),
        )

        assertEquals(firstMissedDate, restoredAnchor)
        assertEquals(firstMissedDate, timelineStartForToday(currentDate, restoredAnchor))
    }

    @Test
    fun firstContactWhoseDueDateAlreadyPassedKeepsTheAnchorAfterRestart() {
        val dueDate = today.minus(DatePeriod(days = 3))
        val notStartedContact = contact("first", "#007AFF", frequency = "daily", nextCheckInDate = dueDate.toString())

        val restoredAnchor = resolveInitialCountdownStartDate(
            existingStartDate = null,
            checkIns = emptyList(),
            contacts = listOf(notStartedContact),
            today = today,
        )

        // The anchor is the durable next-check-in date, not today's ephemeral "due today".
        assertEquals(dueDate, restoredAnchor)
        assertEquals(dueDate, timelineStartForToday(today, restoredAnchor))

        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today, restoredAnchor),
            today = today,
            events = buildCheckInTimelineEvents(
                checkIns = emptyList(),
                contacts = listOf(notStartedContact),
                today = today,
                missedCheckIns = emptyList(),
                initialCountdownStartDate = restoredAnchor,
            ),
        )
        // The overdue date sits in the first cell — not the centered rolling position.
        assertEquals(dueDate, slots.first().date)
        assertTrue(slots.first().hasMissedCheckIn)
        assertTrue(!slots.first().isCheckedIn)
        assertTrue(timelineStartForToday(today, restoredAnchor) != timelineStartForToday(today))
    }

    @Test
    fun notStartedContactWithoutAScheduledDateDefaultsTheAnchorToToday() {
        val anchor = resolveInitialCountdownStartDate(
            existingStartDate = null,
            checkIns = emptyList(),
            contacts = listOf(contact("first", "#007AFF", nextCheckInDate = null)),
            today = today,
        )

        assertEquals(today, anchor)
    }

    @Test
    fun timelineStartsOverAtTheBottomLeftAfterTwentySixChronologicalPositions() {
        val firstCheckInDate = LocalDate(2026, 8, 1)
        val cycleEndDate = firstCheckInDate.plus(DatePeriod(days = TIMELINE_SLOT_COUNT))

        assertEquals(
            cycleEndDate,
            timelineStartForToday(cycleEndDate, firstCheckInDate),
        )
    }

    @Test
    fun futureFirstContactKeepsTodayAsTheFirstTimelineCell() {
        val firstDueDate = today.plus(DatePeriod(days = 7))
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("future", "#007AFF", frequency = "weekly", nextCheckInDate = firstDueDate.toString()),
            ),
            today = today,
        )

        assertEquals(timelineStartForToday(today), timelineStartForToday(today))
    }

    @Test
    fun dailyContactShowsHistoryAndCurrentCheckpointWithoutProjectingFutureCells() {
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("daily", "2026-08-14T12:00:00Z")),
            contacts = listOf(
                contact(
                    id = "daily",
                    avatarColor = "#007AFF",
                    frequency = "daily",
                    nextCheckInDate = "2026-08-15",
                ),
            ),
            today = LocalDate(2026, 8, 15),
        )

        val currentDate = LocalDate(2026, 8, 15)
        val yesterday = currentDate.minus(DatePeriod(days = 1))
        assertEquals(2, events.size)
        assertTrue(events.any { it.date == yesterday && it.checkedIn })
        assertTrue(events.any { it.date == currentDate })
        assertTrue(events.none { it.date == currentDate.plus(DatePeriod(days = 1)) })
    }

    @Test
    fun dailyContactMissedYesterdayRemainsIncompleteInYesterdayCell() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("daily", "#007AFF", frequency = "daily", nextCheckInDate = "2026-08-07"),
            ),
            today = today,
        )

        val yesterday = today.minus(DatePeriod(days = 1))
        val missed = events.single { it.date == yesterday }
        assertEquals(1, missed.avatarCount)
        assertTrue(!missed.checkedIn)

        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        )
        val missedSlot = slots.single { it.date == yesterday }
        assertEquals(0, missedSlot.avatarCount)
        assertTrue(!shouldRenderTimelineAvatars(missedSlot))
        assertTrue(missedSlot.hasMissedCheckIn)
        assertTrue(!missedSlot.isCheckedIn)
        assertEquals(1, slots.single { it.date == today }.avatarCount)
    }

    @Test
    fun outstandingContactsAppearAsPendingAvatarsTodayRegardlessOfAnchor() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("overdue-none", "#FF3B30", frequency = "none", nextCheckInDate = today.minus(DatePeriod(days = 3)).toString()),
                contact("overdue-daily", "#007AFF", frequency = "daily", nextCheckInDate = today.minus(DatePeriod(days = 1)).toString()),
                contact("due-today", "#34C759", frequency = "weekly", nextCheckInDate = today.toString()),
                contact("future", "#FF9500", frequency = "daily", nextCheckInDate = today.plus(DatePeriod(days = 1)).toString()),
            ),
            today = today,
        )

        val todayPending = events.filter { it.date == today }
        assertEquals(3, todayPending.size)
        assertTrue(todayPending.all { !it.checkedIn })
        assertEquals(3, todayPending.sumOf { it.avatarCount })
    }

    @Test
    fun currentDaySlotRendersAvatarStackOfAllOutstandingContacts() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("a", "#FF3B30", frequency = "none", nextCheckInDate = today.minus(DatePeriod(days = 2)).toString()),
                contact("b", "#007AFF", frequency = "daily", nextCheckInDate = today.minus(DatePeriod(days = 1)).toString()),
            ),
            today = today,
        )
        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        )

        val todaySlot = slots.single { it.date == today }
        assertTrue(todaySlot.isCurrent)
        assertTrue(todaySlot.hasPendingCheckIn)
        assertEquals(2, todaySlot.avatarCount)
        assertTrue(shouldRenderTimelineAvatars(todaySlot))
    }

    @Test
    fun missedDayStaysEmptyWithoutPlantOrAvatar() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("overdue", "#007AFF", frequency = "daily", nextCheckInDate = today.minus(DatePeriod(days = 1)).toString()),
            ),
            today = today,
        )
        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        )

        val missedSlot = slots.single { it.date == today.minus(DatePeriod(days = 1)) }
        assertTrue(missedSlot.hasMissedCheckIn)
        assertEquals(0, missedSlot.avatarCount)
    }

    @Test
    fun inactiveEmptyDaysUseTheSmallDotWhileMissedDaysStayFullSize() {
        val events = listOf(
            TimelineEvent(today.minus(DatePeriod(days = 3)), missed = false),
            TimelineEvent(today.minus(DatePeriod(days = 1)), missed = true),
        )
        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        ).associateBy { it.date }

        // A past day with genuinely no check-in activity is an inactive dot.
        val emptyPast = slots.getValue(today.minus(DatePeriod(days = 2)))
        assertTrue(shouldRenderInactiveDayDot(emptyPast))
        assertEquals(0, emptyPast.avatarCount)

        // Future days stay inactive dots (as before).
        assertTrue(shouldRenderInactiveDayDot(slots.getValue(today.plus(DatePeriod(days = 1)))))

        // A real missed day is NOT a dot — it keeps its full-size empty cell.
        val missed = slots.getValue(today.minus(DatePeriod(days = 1)))
        assertTrue(missed.hasMissedCheckIn)
        assertFalse(shouldRenderInactiveDayDot(missed))

        // Today is never drawn as a plain dot.
        assertFalse(shouldRenderInactiveDayDot(slots.getValue(today)))
    }

    @Test
    fun missedOccurrenceDoesNotRemoveCompletedAvatarFromTheSameDate() {
        val yesterday = today.minus(DatePeriod(days = 1))
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("completed", "${yesterday}T12:00:00Z")),
            contacts = listOf(
                contact("missed", "#007AFF", frequency = "daily", nextCheckInDate = yesterday.toString()),
                contact("completed", "#FF3B30", frequency = "daily", nextCheckInDate = today.toString()),
            ),
            today = today,
        )

        val missedDateSlot = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        ).single { it.date == yesterday }

        assertEquals(1, missedDateSlot.avatarCount)
        assertTrue(missedDateSlot.hasMissedCheckIn)
        assertTrue(!missedDateSlot.isCheckedIn)
    }

    @Test
    fun weeklyContactAppearsEverySevenDaysAndNotOnTheDaysBetween() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("weekly", "#007AFF", frequency = "weekly", nextCheckInDate = "2026-08-08"),
            ),
            today = today,
        )

        assertTrue(events.any { it.date == LocalDate(2026, 8, 8) })
        assertTrue(events.none { it.date == LocalDate(2026, 8, 15) })
        assertTrue(events.none { it.date == LocalDate(2026, 8, 9) })
    }

    @Test
    fun biweeklyContactAppearsEveryFourteenDays() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("biweekly", "#007AFF", frequency = "biweekly", nextCheckInDate = "2026-08-01"),
            ),
            today = today,
        )

        assertTrue(events.any { it.date == LocalDate(2026, 8, 1) })
        assertTrue(events.none { it.date == LocalDate(2026, 8, 15) })
        assertTrue(events.none { it.date == LocalDate(2026, 8, 14) })
    }

    @Test
    fun monthlyContactKeepsItsScheduledDayAcrossMonthBoundary() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("monthly", "#007AFF", frequency = "monthly", nextCheckInDate = "2026-08-08"),
            ),
            today = today,
        )

        assertTrue(events.any { it.date == LocalDate(2026, 8, 8) })
        assertTrue(events.none { it.date == LocalDate(2026, 8, 9) })

        val nextMonthEvents = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("monthly", "#007AFF", frequency = "monthly", nextCheckInDate = "2026-08-08"),
            ),
            today = LocalDate(2026, 9, 8),
        )
        assertTrue(nextMonthEvents.any { it.date == LocalDate(2026, 9, 8) })

        val monthEndEvents = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("monthly", "#007AFF", frequency = "monthly", nextCheckInDate = "2026-08-31"),
            ),
            today = LocalDate(2026, 9, 30),
        )

        assertTrue(monthEndEvents.any { it.date == LocalDate(2026, 9, 30) })
    }

    @Test
    fun futureScheduledCheckInDoesNotFillItsCellBeforeItsDate() {
        val futureDate = today.plus(DatePeriod(days = 3))
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("future", "#007AFF", frequency = "weekly", nextCheckInDate = futureDate.toString()),
            ),
            today = today,
        )

        assertTrue(events.none { it.date == futureDate })
        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        )
        assertEquals(0, slots.single { it.date == futureDate }.avatarCount)
        assertTrue(slots.single { it.date == futureDate }.isFuture)
    }

    @Test
    fun firstScheduledOccurrenceStaysA_dotUntilItsDateArrives() {
        val firstDueDate = today.plus(DatePeriod(days = 7))
        val contacts = listOf(
            contact(
                "future",
                "#007AFF",
                frequency = "weekly",
                nextCheckInDate = firstDueDate.toString(),
            ),
        )
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = contacts,
            today = today,
        )

        assertTrue(events.isEmpty())
        val futureSlots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = events,
        )
        val futureSlot = futureSlots.single { it.date == firstDueDate }
        assertEquals(0, futureSlot.avatarCount)
        assertTrue(futureSlot.isFuture)
        assertTrue(!shouldRenderTimelineAvatars(futureSlot))

        val arrivedEvents = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = contacts,
            today = firstDueDate,
        )
        val arrivedSlots = buildTimelineSlots(
            startDate = timelineStartForToday(firstDueDate),
            today = firstDueDate,
            events = arrivedEvents,
        )
        assertEquals(1, arrivedSlots.single { it.date == firstDueDate }.avatarCount)
    }

    @Test
    fun completingTodayDoesNotBackfillEarlierCellsOrShowTheNextOccurrence() {
        val completedDate = today
        val contactBeforeCheckIn = contact(
            "daily",
            "#007AFF",
            frequency = "daily",
            nextCheckInDate = completedDate.toString(),
        )
        val before = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(contactBeforeCheckIn),
            today = today,
        )
        assertEquals(today.minus(DatePeriod(days = 12)), timelineStartForToday(today))

        val contactAfterCheckIn = contactBeforeCheckIn.copy(
            nextCheckInDate = completedDate.plus(DatePeriod(days = 1)).toString(),
            lastCheckInDate = completedDate.toString(),
        )
        val after = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("daily", "2026-08-08T12:00:00Z")),
            contacts = listOf(contactAfterCheckIn),
            today = completedDate,
        )
        val slots = buildTimelineSlots(
            startDate = timelineStartForToday(today),
            today = today,
            events = after,
        )

        val completedSlot = slots.single { it.date == today }
        assertEquals(1, completedSlot.avatarCount)
        assertTrue(completedSlot.isCheckedIn)
        assertTrue(after.none { it.date < today })
        assertEquals(0, slots.single { it.date == today.plus(DatePeriod(days = 1)) }.avatarCount)
    }

    @Test
    fun completedOccurrenceRemainsVisibleAfterNextCheckInDateAdvances() {
        val completedDate = today
        val nextDate = completedDate.plus(DatePeriod(days = 7))
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("weekly", "2026-08-08T12:00:00Z")),
            contacts = listOf(
                contact(
                    "weekly",
                    "#007AFF",
                    frequency = "weekly",
                    nextCheckInDate = nextDate.toString(),
                ).copy(lastCheckInDate = completedDate.toString()),
            ),
            today = completedDate,
        )

        val completed = events.single { it.date == completedDate }
        assertTrue(completed.checkedIn)
        assertEquals(1, completed.avatarCount)
    }

    @Test
    fun multipleContactsDueOnTheSameDayShareOneCheckpoint() {
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("first", "2026-08-08T12:00:00Z")),
            contacts = listOf(
                contact("first", "#007AFF", frequency = "daily", nextCheckInDate = "2026-08-08"),
                contact("second", "#FF3B30", frequency = "weekly", nextCheckInDate = "2026-08-08"),
            ),
            today = today,
        )

        val checkpoints = events.filter { it.date == today }
        assertEquals(2, checkpoints.size)
        assertEquals(1, checkpoints.count { it.checkedIn })
        assertEquals(1, checkpoints.count { !it.checkedIn })
    }

    @Test
    fun scheduledContactDoesNotAppearOnInterveningDays() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("future", "#007AFF", frequency = "weekly", nextCheckInDate = today.toString()),
            ),
            today = today,
        )

        assertTrue(events.none { it.date == LocalDate(2026, 8, 9) })
    }

    @Test
    fun contactIsDueAgainAfterAnIncompletePreviousCheckpoint() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("daily", "#007AFF", frequency = "daily", nextCheckInDate = "2026-08-07"),
            ),
            today = today,
        )

        assertEquals(1, events.single { it.date == today }.avatarCount)
        assertTrue(events.none { it.date == today.plus(DatePeriod(days = 1)) })
    }

    @Test
    fun sameDayCheckInEventWinsOverStaleOutstandingContactCache() {
        val events = buildCheckInTimelineEvents(
            checkIns = listOf(checkIn("c1", "2026-08-08T12:00:00Z")),
            contacts = listOf(contact("c1", "#007AFF", nextCheckInDate = "2026-08-08")),
            today = today,
        )

        val slots = buildTimelineSlots(
            startDate = today,
            today = today,
            events = events,
        )
        val current = slots.single { it.date == today }

        assertTrue(current.isCheckedIn)
        assertTrue(!current.hasPendingCheckIn)
    }

    @Test
    fun cachedTodayCheckInStillShowsCheckedCalendarStateWhenEventReadIsUnavailable() {
        val events = buildCheckInTimelineEvents(
            checkIns = emptyList(),
            contacts = listOf(
                contact("c1", "#007AFF", nextCheckInDate = "2026-08-08")
                    .copy(lastCheckInDate = "2026-08-08"),
            ),
            today = today,
        )

        val current = buildTimelineSlots(
            startDate = today,
            today = today,
            events = events,
        ).single { it.date == today }

        assertTrue(current.isCheckedIn)
        assertTrue(!current.hasPendingCheckIn)
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
        assertEquals(-10f, offset.x.value, 0.001f)
        assertEquals(5f, offset.y.value, 0.001f)
    }

    @Test
    fun calendarUsesFiftyDpCellsAtDesignWidth() {
        assertEquals(50.dp, timelineCellSizeForWidth(maxWidth = 500.dp, horizontalSpacing = 8.dp))
        assertEquals(44.57143.dp, timelineCellSizeForWidth(maxWidth = 360.dp, horizontalSpacing = 8.dp))
    }

    @Test
    fun calendarGrowsAtMediumAndExpandedWidthsWithoutStretchingIndefinitely() {
        assertEquals(50.dp, timelineMaxCellSizeForWidth(599.dp))
        assertEquals(56.dp, timelineMaxCellSizeForWidth(600.dp))
        assertEquals(56.dp, timelineCellSizeForWidth(maxWidth = 720.dp))
        assertEquals(64.dp, timelineMaxCellSizeForWidth(840.dp))
        assertEquals(64.dp, timelineCellSizeForWidth(maxWidth = 1200.dp))
    }

    @Test
    fun largerCalendarCellsScaleAvatarsAndPendingBubble() {
        assertEquals(44.8.dp, timelineAvatarSize(index = 0, visibleCount = 1, cellSize = 56.dp))
        assertEquals(35.84.dp, timelineAvatarSize(index = 1, visibleCount = 2, cellSize = 56.dp))
        assertEquals(107.52f, timelineBubbleSize(56.dp).value, 0.001f)
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
    fun timelineStartsTwelveDaysBeforeToday() {
        assertEquals(LocalDate(2026, 8, 19), timelineStartForToday(LocalDate(2026, 8, 31)))
        assertEquals(LocalDate(2026, 8, 20), timelineStartForToday(LocalDate(2026, 9, 1)))
    }


    @Test
    fun timelineCrossesMonthBoundaryWithoutResettingSlots() {
        val slots = buildTimelineSlots(
            startDate = LocalDate(2026, 8, 27),
            today = LocalDate(2026, 8, 31),
            events = listOf(
                TimelineEvent(LocalDate(2026, 8, 31), checkedIn = true, avatarCount = 1),
                TimelineEvent(LocalDate(2026, 9, 1), checkedIn = true, avatarCount = 1),
            ),
        )

        assertEquals(LocalDate(2026, 8, 31), slots[4].date)
        assertEquals(LocalDate(2026, 9, 1), slots[5].date)
        assertEquals(1, slots[4].avatarCount)
        assertEquals(1, slots[5].avatarCount)
    }

    @Test
    fun checkInPositionStaysOnItsDaySlotAsTodayChanges() {
        val events = listOf(
            TimelineEvent(LocalDate(2026, 8, 10), checkedIn = true, avatarCount = 1),
            TimelineEvent(LocalDate(2026, 8, 30), checkedIn = true, avatarCount = 1),
        )

        val earlyMonthSlots = buildTimelineSlots(
            startDate = LocalDate(2026, 8, 8),
            today = LocalDate(2026, 8, 8),
            events = events,
        )
        val lateMonthSlots = buildTimelineSlots(
            startDate = LocalDate(2026, 8, 26),
            today = LocalDate(2026, 8, 26),
            events = events,
        )

        assertEquals(1, earlyMonthSlots[2].avatarCount)
        assertEquals(0, lateMonthSlots[2].avatarCount)
        assertEquals(1, earlyMonthSlots[22].avatarCount)
        assertEquals(1, lateMonthSlots[4].avatarCount)
    }

    @Test
    fun timelineDateAdvancesWhenTheClockMovesToTheNextDay() {
        val nextDay = today.plus(DatePeriod(days = 1))

        assertEquals(today, updateTimelineDate(today, today))
        assertEquals(nextDay, updateTimelineDate(today, nextDay))
    }

    @Test
    fun calendarIncompleteRowsKeepTheirOriginalAlignment() {
        assertEquals(3, timelineRowLeadingEmptyColumns(visualRow = 4))
        assertEquals(0, timelineRowLeadingEmptyColumns(visualRow = 0))
    }

    @Test
    fun loadingCalendarUsesTheSameFiveRowsAndTwentySixVisibleCellsAsTheNormalCalendar() {
        assertEquals(listOf(4, 7, 7, 7, 1), homeLoadingTimelineRowSlotCounts())
        assertEquals(26, homeLoadingTimelineRowSlotCounts().sum())
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
        frequency: String = "weekly",
        nextCheckInDate: String? = "2026-08-09",
    ) = Contact(
        id = id,
        name = id,
        avatarColor = avatarColor,
        checkInFrequency = frequency,
        reminderTime = null,
        nextCheckInDate = nextCheckInDate,
        lastCheckInDate = null,
        streakCount = 0,
    )
}