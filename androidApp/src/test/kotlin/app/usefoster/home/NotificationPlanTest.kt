package app.usefoster.home

import app.usefoster.home.domain.CheckInDue
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.OverdueCheckIn
import app.usefoster.home.domain.Reminder
import app.usefoster.home.domain.buildDayPlans
import app.usefoster.home.domain.buildDuePlan
import app.usefoster.home.domain.nextOccurrence
import app.usefoster.shared.notifications.MaxScheduledDays
import app.usefoster.shared.notifications.NotificationTapRouter
import app.usefoster.shared.notifications.NotificationTarget
import app.usefoster.shared.notifications.NotificationCategories
import app.usefoster.shared.notifications.ReconcileSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val UTC = TimeZone.UTC

private fun utcMillis(year: Int, month: Int, day: Int, hour: Int = 0): Long =
    LocalDate(year, month, day).atTime(hour, 0).toInstant(UTC).toEpochMilliseconds()

private fun utcMillisFromDay(epochDay: Long): Long =
    LocalDate.fromEpochDays(epochDay).atTime(12, 0).toInstant(UTC).toEpochMilliseconds()

private fun contact(
    id: String,
    name: String = id,
    nextCheckInDate: String? = null,
    reminderTime: String? = "09:00:00",
) = Contact(
    id = id,
    name = name,
    avatarColor = null,
    checkInFrequency = "weekly",
    reminderTime = reminderTime,
    nextCheckInDate = nextCheckInDate,
    lastCheckInDate = null,
    streakCount = 0,
)

private fun reminder(
    id: String,
    recurrence: String = "none",
    dateEpochMillis: Long? = null,
    contactId: String = "c1",
) = Reminder(
    id = id,
    contactId = contactId,
    title = "R-$id",
    description = "",
    recurrence = recurrence,
    dateEpochMillis = dateEpochMillis,
)

class NotificationPlanTest {

    private val today = LocalDate(2026, 8, 25)
    private val now = utcMillis(2026, 8, 25, 12)

    @Test
    fun dueTodayContactGoesIntoTodayBucket() {
        val plan = buildDuePlan(
            contacts = listOf(contact("c1", nextCheckInDate = "2026-08-25")),
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
        )
        val items = plan.dayItems[today.toEpochDays()].orEmpty()
        assertEquals(1, items.size)
        val item = items.first()
        assertTrue(item is CheckInDue)
        assertEquals("c1", item.contactId)
    }

    @Test
    fun overdueContactSynthesizesIntoTodayBucketWithRisingPriority() {
        val plan = buildDuePlan(
            contacts = listOf(contact("c1", nextCheckInDate = "2026-08-18")),
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
        )
        val items = plan.dayItems[today.toEpochDays()].orEmpty()
        val overdue = items.first()
        assertTrue(overdue is OverdueCheckIn)
        overdue as OverdueCheckIn
        assertEquals(7, overdue.daysOverdue)
        assertTrue(overdue.priority > 300)
    }

    @Test
    fun futureContactBucketsOnItsOwnDay() {
        val plan = buildDuePlan(
            contacts = listOf(contact("c1", nextCheckInDate = "2026-08-28")),
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
        )
        assertTrue(plan.dayItems[today.toEpochDays()].isNullOrEmpty())
        assertEquals(1, plan.dayItems[LocalDate(2026, 8, 28).toEpochDays()].orEmpty().size)
    }

    @Test
    fun checkedInTodayContactIsExcluded() {
        val plan = buildDuePlan(
            contacts = listOf(contact("c1", nextCheckInDate = "2026-08-25")),
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
            checkedInTodayContactIds = setOf("c1"),
        )
        assertTrue(plan.dayItems[today.toEpochDays()].isNullOrEmpty())
    }

    @Test
    fun oneShotCustomReminderExpandsToItsDateAtNine() {
        val occurrence = reminder("r1", recurrence = "none", dateEpochMillis = utcMillis(2026, 9, 1))
            .nextOccurrence(now, UTC)
        assertEquals(utcMillis(2026, 9, 1, 9), occurrence)
    }

    @Test
    fun weeklyCustomReminderRollsForwardFromAnchor() {
        // Anchor: Aug 3 2026. Weekly steps: Aug 3/10/17/24/31 — Aug 31 09:00 is
        // the first occurrence after now (Aug 25 12:00).
        val occurrence = reminder("r1", recurrence = "weekly", dateEpochMillis = utcMillis(2026, 8, 3))
            .nextOccurrence(now, UTC)
        assertEquals(utcMillis(2026, 8, 31, 9), occurrence)
    }

    @Test
    fun groupedDayHeadlinesHighestPriorityAndDropsSingleTarget() {
        val plan = buildDuePlan(
            contacts = listOf(
                contact("due", nextCheckInDate = "2026-08-25"),
                contact("overdue", nextCheckInDate = "2026-08-20"),
            ),
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
        )
        val dayPlans = buildDayPlans(plan.dayItems, timeZone = UTC)
        assertEquals(1, dayPlans.size)
        val digest = dayPlans.first()
        assertEquals(2, digest.itemCount)
        assertEquals("overdue", digest.headline)
        assertEquals(NotificationCategories.MISSED_CHECK_INS, digest.category)
        assertNull(digest.singleTargetId)
    }

    @Test
    fun singleItemDayKeepsPerContactCopyTarget() {
        val plan = buildDuePlan(
            contacts = listOf(contact("solo", nextCheckInDate = "2026-08-26")),
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
        )
        val digest = buildDayPlans(plan.dayItems, timeZone = UTC).single()
        assertEquals(NotificationCategories.CHECK_INS, digest.category)
        assertEquals("solo", digest.singleTargetId)
        assertTrue(digest.singleIsContact)
    }

    @Test
    fun dayPlansAreCappedAtMaxScheduledDays() {
        val baseDay = LocalDate(2026, 8, 26).toEpochDays()
        val contacts = (0 until MaxScheduledDays + 10).map { index ->
            contact("c$index", nextCheckInDate = LocalDate.fromEpochDays(baseDay + index).toString())
        }
        val plan = buildDuePlan(
            contacts = contacts,
            customReminders = emptyList(),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
            horizonDays = MaxScheduledDays + 10,
        )
        val dayPlans = buildDayPlans(plan.dayItems, timeZone = UTC)
        assertEquals(MaxScheduledDays, dayPlans.size)
    }

    @Test
    fun digestDecisionTable() {
        val dayKey = today.toEpochDays()
        val fireAt = utcMillis(2026, 8, 25, 9)
        val quietCutoff = utcMillis(2026, 8, 25, 21)
        val lateNight = utcMillis(2026, 8, 25, 22)
        val afternoon = utcMillis(2026, 8, 25, 14)
        val earlyMorning = utcMillis(2026, 8, 25, 7)

        val scheduled = app.usefoster.shared.notifications.decideDigestSchedule(
            dayKey, fireAt, earlyMorning, quietCutoff, deliveredDays = emptySet(),
            source = ReconcileSource.FOREGROUND,
        )
        assertEquals(
            app.usefoster.shared.notifications.DigestDecision.Schedule(fireAt, isCatchUp = false),
            scheduled,
        )

        val delivered = app.usefoster.shared.notifications.decideDigestSchedule(
            dayKey, fireAt, now, quietCutoff, deliveredDays = setOf(dayKey),
            source = ReconcileSource.BACKGROUND,
        )
        assertEquals(app.usefoster.shared.notifications.DigestDecision.SkipDelivered, delivered)

        val surface = app.usefoster.shared.notifications.decideDigestSchedule(
            dayKey, fireAt, afternoon, quietCutoff,
            deliveredDays = emptySet(), source = ReconcileSource.FOREGROUND,
        )
        assertEquals(app.usefoster.shared.notifications.DigestDecision.SurfaceInApp, surface)

        val catchUp = app.usefoster.shared.notifications.decideDigestSchedule(
            dayKey, fireAt, afternoon, quietCutoff,
            deliveredDays = emptySet(), source = ReconcileSource.BACKGROUND,
        )
        assertTrue(catchUp is app.usefoster.shared.notifications.DigestDecision.Schedule)
        catchUp as app.usefoster.shared.notifications.DigestDecision.Schedule
        assertTrue(catchUp.isCatchUp)
        assertEquals(afternoon + 120_000L, catchUp.fireAtEpochMillis)

        val deferred = app.usefoster.shared.notifications.decideDigestSchedule(
            dayKey, fireAt, lateNight, quietCutoff,
            deliveredDays = emptySet(), source = ReconcileSource.BACKGROUND,
        )
        assertEquals(app.usefoster.shared.notifications.DigestDecision.DeferToTomorrow, deferred)
    }

    // -------------------------------------------------------------------
    // D1 granularity split: custom reminders are standalone, never digest.
    // -------------------------------------------------------------------

    @Test
    fun customRemindersRouteOnlyToStandalonesNeverDigestBuckets() {
        val plan = buildDuePlan(
            contacts = listOf(contact("c1", nextCheckInDate = "2026-08-25")),
            customReminders = listOf(
                reminder("r1", recurrence = "none", dateEpochMillis = utcMillis(2026, 9, 1)),
            ),
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
        )
        // Every digest item is day-granular; no reminder leaked into a bucket.
        assertTrue(plan.dayItems.values.flatten().all { it is CheckInDue || it is OverdueCheckIn })
        assertEquals(1, plan.standalones.size)
        val standalone = plan.standalones.single()
        assertEquals("r1:${LocalDate(2026, 9, 1).toEpochDays()}", standalone.key)
        assertEquals(utcMillis(2026, 9, 1, 9), standalone.fireAtEpochMillis)
    }

    @Test
    fun timeSpecificReminderFiresAtItsOwnClockTimeNotTheDigestHour() {
        // "Take medication at 8pm" must fire at 20:00 — never be pulled to the
        // 9am digest (the exact regression the review caught).
        val medication = reminder("med", recurrence = "none", dateEpochMillis = utcMillis(2026, 9, 1))
            .copy(timeOfDay = "20:00")
        assertEquals(utcMillis(2026, 9, 1, 20), medication.nextOccurrence(now, UTC))
    }

    @Test
    fun malformedTimeOfDayFallsBackToDefaultCustomReminderHour() {
        val bad = reminder("bad", recurrence = "none", dateEpochMillis = utcMillis(2026, 9, 1))
            .copy(timeOfDay = "25:99")
        assertEquals(utcMillis(2026, 9, 1, 9), bad.nextOccurrence(now, UTC))
    }

    // -------------------------------------------------------------------
    // Combined iOS 64-notification budget: digest days + standalones.
    // -------------------------------------------------------------------

    @Test
    fun combinedDigestAndStandaloneBudgetNeverExceedsMaxScheduledDays() {
        val baseDay = LocalDate(2026, 8, 26).toEpochDays()
        val contacts = (0 until 30).map { index ->
            contact("c$index", nextCheckInDate = LocalDate.fromEpochDays(baseDay + index).toString())
        }
        // Each custom reminder contributes exactly one pending occurrence, so
        // overflow requires many distinct reminders: 50 one-shots across the
        // horizon against 30 digest days = 80 candidate notifications > 64.
        val reminders = (0 until 50).map { index ->
            reminder(
                "r$index",
                recurrence = "none",
                dateEpochMillis = utcMillisFromDay(baseDay + index),
            )
        }
        val plan = buildDuePlan(
            contacts = contacts,
            customReminders = reminders,
            today = today,
            nowEpochMillis = now,
            timeZone = UTC,
            horizonDays = 45,
        )
        val digestDays = plan.dayItems.size
        assertEquals(30, digestDays)
        // Standalones fill exactly the remaining budget, soonest kept first.
        assertEquals(MaxScheduledDays - digestDays, plan.standalones.size)
        assertTrue(
            plan.standalones.zipWithNext().all { (a, b) -> a.fireAtEpochMillis <= b.fireAtEpochMillis },
        )
        val dayPlans = buildDayPlans(plan.dayItems, timeZone = UTC)
        assertEquals(MaxScheduledDays, dayPlans.size + plan.standalones.size)
    }

    // -------------------------------------------------------------------
    // Timezone / DST re-bucketing (plan §9).
    // -------------------------------------------------------------------

    @Test
    fun digestFireTimeShiftsWithTimeZone() {
        val dayKey = LocalDate(2026, 8, 26).toEpochDays()
        val items = mapOf(dayKey to listOf(CheckInDue("c1", "C1")))
        val ny = TimeZone.of("America/New_York")
        val utcPlan = buildDayPlans(items, timeZone = UTC).single()
        val nyPlan = buildDayPlans(items, timeZone = ny).single()
        // August = EDT (UTC-4): the same local 9am fires at 13:00 UTC,
        // i.e. 4h LATER in epoch terms than 9am UTC.
        assertEquals(4 * 60 * 60 * 1000L, nyPlan.fireAtEpochMillis - utcPlan.fireAtEpochMillis)
    }

    @Test
    fun dstTransitionMovesTheUtcOffset() {
        val ny = TimeZone.of("America/New_York")
        val summer = buildDayPlans(
            mapOf(LocalDate(2026, 8, 26).toEpochDays() to listOf(CheckInDue("c1", "C1"))),
            timeZone = ny,
        ).single()
        val winter = buildDayPlans(
            mapOf(LocalDate(2026, 11, 3).toEpochDays() to listOf(CheckInDue("c1", "C1"))),
            timeZone = ny,
        ).single()
        assertEquals(4 * 60 * 60 * 1000L, summer.fireAtEpochMillis - utcMillis(2026, 8, 26, 9))
        assertEquals(5 * 60 * 60 * 1000L, winter.fireAtEpochMillis - utcMillis(2026, 11, 3, 9))
    }

    // -------------------------------------------------------------------
    // Cold-start tap replay buffer.
    // -------------------------------------------------------------------

    @Test
    fun tapRouterBuffersColdStartTapUntilConsumedOnce() {
        NotificationTapRouter.post(NotificationTarget(dayKey = 7L, contactId = "c1"))
        assertEquals(NotificationTarget(dayKey = 7L, contactId = "c1"), NotificationTapRouter.consume())
        assertNull(NotificationTapRouter.consume())
    }
}
