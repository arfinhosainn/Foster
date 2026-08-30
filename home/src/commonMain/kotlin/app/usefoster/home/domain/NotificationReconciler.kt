package app.usefoster.home.domain

import app.usefoster.shared.notifications.CatchUpDelayMillis
import app.usefoster.shared.notifications.DayPlan
import app.usefoster.shared.notifications.DigestDecision
import app.usefoster.shared.notifications.NotificationPlanState
import app.usefoster.shared.notifications.NotificationPlanStore
import app.usefoster.shared.notifications.NotificationSchedulingOps
import app.usefoster.shared.notifications.ReconcileSource
import app.usefoster.shared.notifications.StandalonePlan
import app.usefoster.shared.notifications.decideDigestSchedule
import app.usefoster.shared.notifications.defaultNotificationPlanStore
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Applies a [DuePlan] to the scheduler idempotently (plan §3.3/§6):
 *
 *  1. Loads the persisted plan state (delivered/pending sets + snapshot).
 *  2. Decides per day via [decideDigestSchedule] and per standalone key.
 *  3. Persists the NEW state BEFORE touching alarms — a process kill
 *     mid-reconcile can then only leave stale alarms, never zero alarms; the
 *     next reconcile or boot re-arm heals from the snapshot.
 *  4. Applies alarm operations (cancel stale, schedule fresh).
 *
 * Days/keys no longer planned are cancelled so a cleared day never fires an
 * empty digest (plan §7 D3).
 */
class NotificationReconciler(
    private val scheduler: NotificationSchedulingOps,
    private val store: NotificationPlanStore = defaultNotificationPlanStore(),
) {

    suspend fun reconcile(
        plan: DuePlan,
        nowEpochMillis: Long,
        source: ReconcileSource,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        digestHour: Int = DEFAULT_DIGEST_HOUR,
    ) {
        val state = store.load()
        val today = Instant.fromEpochMilliseconds(nowEpochMillis)
            .toLocalDateTime(timeZone).date
        val todayKey = today.toEpochDays()
        val quietCutoff = runCatching {
            LocalDateTime(today.year, today.month, today.day, QuietCutoffHourLocal, 0, 0)
                .toInstant(timeZone)
                .toEpochMilliseconds()
        }.getOrDefault(nowEpochMillis)

        val dayPlans = buildDayPlans(
            dayItems = plan.dayItems,
            digestHour = digestHour,
            timeZone = timeZone,
        )

        val deliveredDays = state.deliveredDays.toMutableSet()
        val deliveredKeys = state.deliveredKeys.toMutableSet()
        val pendingDays = mutableSetOf<Long>()
        val pendingKeys = mutableSetOf<String>()
        val newDays = mutableListOf<DayPlan>()
        val newStandalones = mutableListOf<StandalonePlan>()
        val ops = mutableListOf<suspend () -> Unit>()

        dayPlans.forEach { dayPlan ->
            when (
                val decision = decideDigestSchedule(
                    dayKey = dayPlan.dayKey,
                    fireAtEpochMillis = dayPlan.fireAtEpochMillis,
                    nowEpochMillis = nowEpochMillis,
                    quietCutoffEpochMillis = quietCutoff,
                    deliveredDays = state.deliveredDays,
                    source = source,
                )
            ) {
                is DigestDecision.Schedule -> {
                    // Schedule carries the EFFECTIVE fire time: the planned
                    // hour normally, or now+delta for a coalesced catch-up.
                    // Scheduling the original elapsed hour would be dropped
                    // by both platforms as "in the past".
                    val effective = dayPlan.copy(fireAtEpochMillis = decision.fireAtEpochMillis)
                    pendingDays += dayPlan.dayKey
                    newDays += effective
                    ops += {
                        scheduler.cancelDay(dayPlan.dayKey)
                        scheduler.scheduleDay(effective)
                    }
                    // Background catch-up alarms are marked delivered by the
                    // platform when they actually fire/post (Android receiver)
                    // or at schedule time (iOS actual).
                }

                DigestDecision.SurfaceInApp -> {
                    deliveredDays += dayPlan.dayKey
                    ops += { scheduler.cancelDay(dayPlan.dayKey) }
                }

                DigestDecision.SkipDelivered,
                DigestDecision.DeferToTomorrow,
                -> ops += { scheduler.cancelDay(dayPlan.dayKey) }
            }
        }

        plan.standalones.forEach { standalone ->
            when {
                standalone.key in state.deliveredKeys -> Unit

                standalone.fireAtEpochMillis > nowEpochMillis -> {
                    pendingKeys += standalone.key
                    newStandalones += standalone
                    ops += {
                        scheduler.cancelStandalone(standalone.key)
                        scheduler.scheduleStandalone(standalone)
                    }
                }

                nowEpochMillis <= quietCutoff && source == ReconcileSource.BACKGROUND -> {
                    val catchUp = standalone.copy(
                        fireAtEpochMillis = nowEpochMillis + CatchUpDelayMillis,
                    )
                    pendingKeys += standalone.key
                    newStandalones += catchUp
                    ops += { scheduler.scheduleStandalone(catchUp) }
                }

                // Foreground + elapsed: the user is in the app — surface in-app
                // only, mark delivered, never buzz (plan §3.3).
                nowEpochMillis <= quietCutoff -> deliveredKeys += standalone.key

                // Past quiet cutoff: roll to the next occurrence silently.
                else -> Unit
            }
        }

        (state.pendingDays - pendingDays).forEach { day ->
            ops += { scheduler.cancelDay(day) }
        }
        (state.pendingKeys - pendingKeys).forEach { key ->
            ops += { scheduler.cancelStandalone(key) }
        }

        deliveredDays.removeAll { it < todayKey }

        store.save(
            NotificationPlanState(
                deliveredDays = deliveredDays,
                deliveredKeys = deliveredKeys,
                pendingDays = pendingDays,
                pendingKeys = pendingKeys,
                days = newDays,
                standalones = newStandalones,
            ),
        )
        ops.forEach { it() }
    }

    private companion object {
        const val QuietCutoffHourLocal = 21
    }
}
