package app.usenekko.home

import app.usenekko.home.domain.CheckInDue
import app.usenekko.home.domain.DuePlan
import app.usenekko.home.domain.NotificationReconciler
import app.usenekko.shared.notifications.DayPlan
import app.usenekko.shared.notifications.NotificationCategories
import app.usenekko.shared.notifications.NotificationPlanState
import app.usenekko.shared.notifications.NotificationPlanStore
import app.usenekko.shared.notifications.NotificationSchedulingOps
import app.usenekko.shared.notifications.ReconcileSource
import app.usenekko.shared.notifications.StandalonePlan
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val UTC = TimeZone.UTC

private fun utcMillis(year: Int, month: Int, day: Int, hour: Int = 0): Long =
    LocalDate(year, month, day).atTime(hour, 0).toInstant(UTC).toEpochMilliseconds()

/** Records every scheduling op in order so ops vs snapshot-write ordering is observable. */
private class RecordingScheduler(private val log: MutableList<String>) : NotificationSchedulingOps {
    val scheduledDays = mutableListOf<DayPlan>()

    override suspend fun scheduleDay(plan: DayPlan) {
        log += "scheduleDay:${plan.dayKey}"
        scheduledDays += plan
    }

    override suspend fun scheduleStandalone(plan: StandalonePlan) {
        log += "scheduleStandalone:${plan.key}"
    }

    override suspend fun cancelDay(dayKey: Long) {
        log += "cancelDay:$dayKey"
    }

    override suspend fun cancelStandalone(key: String) {
        log += "cancelStandalone:$key"
    }
}

/** Logs saves into the SAME list the scheduler uses: interleaving proves ordering. */
private class InMemoryStore(
    initial: NotificationPlanState = NotificationPlanState(),
    private val log: MutableList<String> = mutableListOf(),
) : NotificationPlanStore {
    var state: NotificationPlanState = initial
        private set

    override suspend fun load(): NotificationPlanState = state

    override suspend fun save(state: NotificationPlanState) {
        this.state = state
        log += "save"
    }
}

class NotificationReconcilerTest {

    private val today = LocalDate(2026, 8, 25)
    private val todayKey = today.toEpochDays()
    private val tomorrowKey = today.plus(1, DateTimeUnit.DAY).toEpochDays()
    private val now = utcMillis(2026, 8, 25, 12)

    @Test
    fun snapshotIsWrittenBeforeAnyAlarmIsTouched() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(log = log)
        val scheduler = RecordingScheduler(log)
        val plan = DuePlan(
            dayItems = mapOf(tomorrowKey to listOf(CheckInDue("c1", "C1"))),
            standalones = listOf(
                StandalonePlan(
                    key = "r1:$tomorrowKey",
                    fireAtEpochMillis = utcMillis(2026, 8, 26, 9),
                    title = "R",
                    category = NotificationCategories.CUSTOM_REMINDERS,
                    targetId = "c1",
                    targetIsContact = true,
                ),
            ),
        )

        NotificationReconciler(scheduler, store)
            .reconcile(plan, now, ReconcileSource.FOREGROUND, UTC)

        // The single atomic snapshot write precedes every alarm operation —
        // a mid-reconcile process kill can only leave stale alarms, never zero.
        assertEquals("save", log.first())
        assertEquals(
            listOf(
                "save",
                "cancelDay:$tomorrowKey",
                "scheduleDay:$tomorrowKey",
                "cancelStandalone:r1:$tomorrowKey",
                "scheduleStandalone:r1:$tomorrowKey",
            ),
            log,
        )
    }

    @Test
    fun lastItemClearedCancelsThePendingDay() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(
            NotificationPlanState(
                pendingDays = setOf(tomorrowKey),
                days = listOf(
                    DayPlan(
                        dayKey = tomorrowKey,
                        fireAtEpochMillis = utcMillis(2026, 8, 26, 9),
                        itemCount = 1,
                        headline = "C1",
                        category = NotificationCategories.CHECK_INS,
                    ),
                ),
            ),
            log,
        )

        NotificationReconciler(RecordingScheduler(log), store)
            .reconcile(DuePlan(emptyMap(), emptyList()), now, ReconcileSource.FOREGROUND, UTC)

        assertTrue("cancelDay:$tomorrowKey" in log)
        assertTrue(store.state.pendingDays.isEmpty())
        assertTrue(store.state.days.isEmpty())
    }

    @Test
    fun staleStandaloneIsCancelledWhenRemovedFromPlan() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(NotificationPlanState(pendingKeys = setOf("r1:1")), log)

        NotificationReconciler(RecordingScheduler(log), store)
            .reconcile(DuePlan(emptyMap(), emptyList()), now, ReconcileSource.FOREGROUND, UTC)

        assertTrue("cancelStandalone:r1:1" in log)
        assertTrue(store.state.pendingKeys.isEmpty())
    }

    @Test
    fun deliveredPastDaysArePrunedSoStillOverdueItemsRenotify() = runTest {
        val yesterdayKey = todayKey - 1
        val store = InMemoryStore(NotificationPlanState(deliveredDays = setOf(yesterdayKey)))

        NotificationReconciler(RecordingScheduler(mutableListOf()), store)
            .reconcile(DuePlan(emptyMap(), emptyList()), now, ReconcileSource.FOREGROUND, UTC)

        assertFalse(yesterdayKey in store.state.deliveredDays)
    }

    @Test
    fun deliveredDayIsNeverRescheduled() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(NotificationPlanState(deliveredDays = setOf(todayKey)), log)
        val plan = DuePlan(mapOf(todayKey to listOf(CheckInDue("c1", "C1"))), emptyList())

        NotificationReconciler(RecordingScheduler(log), store)
            .reconcile(plan, now, ReconcileSource.FOREGROUND, UTC)

        assertFalse(log.any { it.startsWith("scheduleDay") })
        assertTrue(todayKey in store.state.deliveredDays)
    }

    @Test
    fun foregroundElapsedDigestSurfacesInAppWithoutBuzzing() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(log = log)
        // Digest hour (09:00) already passed; still inside the quiet window.
        val plan = DuePlan(mapOf(todayKey to listOf(CheckInDue("c1", "C1"))), emptyList())

        NotificationReconciler(RecordingScheduler(log), store)
            .reconcile(plan, now, ReconcileSource.FOREGROUND, UTC)

        assertTrue("cancelDay:$todayKey" in log)
        assertFalse(log.any { it.startsWith("scheduleDay") })
        assertTrue(todayKey in store.state.deliveredDays)
    }

    @Test
    fun backgroundElapsedDigestSchedulesCoalescedCatchUpAtNowPlusDelta() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(log = log)
        val scheduler = RecordingScheduler(log)
        val plan = DuePlan(mapOf(todayKey to listOf(CheckInDue("c1", "C1"))), emptyList())

        NotificationReconciler(scheduler, store)
            .reconcile(plan, now, ReconcileSource.BACKGROUND, UTC)

        assertTrue(log.any { it == "scheduleDay:$todayKey" })
        assertTrue(todayKey in store.state.pendingDays)
        // The persisted/scheduled day carries the coalesced time, not the
        // already-elapsed digest hour (which both platforms would drop).
        assertEquals(now + 120_000L, store.state.days.single().fireAtEpochMillis)
        assertEquals(now + 120_000L, scheduler.scheduledDays.single().fireAtEpochMillis)
    }

    @Test
    fun foregroundElapsedStandaloneMarksDeliveredWithoutBuzzing() = runTest {
        val log = mutableListOf<String>()
        val store = InMemoryStore(log = log)
        val elapsedKey = "r1:$todayKey"
        val plan = DuePlan(
            emptyMap(),
            listOf(
                StandalonePlan(
                    key = elapsedKey,
                    fireAtEpochMillis = utcMillis(2026, 8, 25, 9),
                    title = "R",
                    category = NotificationCategories.CUSTOM_REMINDERS,
                    targetId = "c1",
                    targetIsContact = true,
                ),
            ),
        )

        NotificationReconciler(RecordingScheduler(log), store)
            .reconcile(plan, now, ReconcileSource.FOREGROUND, UTC)

        assertFalse(log.any { it.startsWith("scheduleStandalone") })
        assertTrue(elapsedKey in store.state.deliveredKeys)
    }

    @Test
    fun pastQuietCutoffDigestDefersSilentlyAndStaysOutstanding() = runTest {
        val lateNight = utcMillis(2026, 8, 25, 22)
        val log = mutableListOf<String>()
        val store = InMemoryStore(log = log)
        val plan = DuePlan(mapOf(todayKey to listOf(CheckInDue("c1", "C1"))), emptyList())

        NotificationReconciler(RecordingScheduler(log), store)
            .reconcile(plan, lateNight, ReconcileSource.FOREGROUND, UTC)

        // Cancel tonight's alarm; the item is NOT marked delivered, so
        // buildDuePlan re-buckets it into tomorrow's digest for a fresh nudge.
        assertTrue("cancelDay:$todayKey" in log)
        assertFalse(log.any { it.startsWith("scheduleDay") })
        assertFalse(todayKey in store.state.deliveredDays)
    }
}
