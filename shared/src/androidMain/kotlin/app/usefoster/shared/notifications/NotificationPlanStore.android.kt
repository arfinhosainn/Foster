package app.usefoster.shared.notifications

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import app.usefoster.shared.notifications.DigestDecision
import app.usefoster.shared.notifications.decideDigestSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [NotificationPlanStore]. The snapshot is written on every
 * reconcile and read by [ReminderBootReceiver] to re-arm alarms after reboot,
 * app update, or timezone/time changes — no network or auth required.
 */
private val Context.notificationPlanDataStore by preferencesDataStore(name = "notification_plan")

private val PLAN_STATE_KEY = stringPreferencesKey("plan_state_json")

private val planJson = Json { ignoreUnknownKeys = true }

actual fun defaultNotificationPlanStore(): NotificationPlanStore =
    AndroidNotificationPlanStore

object AndroidNotificationPlanStore : NotificationPlanStore {

    override suspend fun load(): NotificationPlanState {
        val context = ReminderScheduler.appContext ?: return NotificationPlanState()
        return withContext(Dispatchers.IO) {
            runCatching {
                context.notificationPlanDataStore.data
                    .first()[PLAN_STATE_KEY]
                    ?.let { raw ->
                        runCatching { planJson.decodeFromString<NotificationPlanState>(raw) }
                            .getOrDefault(NotificationPlanState())
                    }
                    ?: NotificationPlanState()
            }.getOrDefault(NotificationPlanState())
        }
    }

    override suspend fun save(state: NotificationPlanState) {
        val context = ReminderScheduler.appContext ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                context.notificationPlanDataStore.edit { prefs ->
                    prefs[PLAN_STATE_KEY] = planJson.encodeToString(
                        NotificationPlanState.serializer(),
                        state,
                    )
                }
            }
        }
    }
}

/**
 * Re-arms alarms from the persisted snapshot without touching the network.
 * Elapsed digests follow the same decision table as the reconciler with
 * [ReconcileSource.BACKGROUND] (coalesced catch-up within the quiet window,
 * overnight rollover past it). Runs after reboot, app update, and
 * timezone/time changes.
 */
object ReminderReArm {

    suspend fun reArmFromSnapshot(nowEpochMillis: Long, timeZoneTodayKey: Long) {
        val scheduler = ReminderScheduler()
        val state = defaultNotificationPlanStore().load()
        if (state.days.isEmpty() && state.standalones.isEmpty()) return

        val quietCutoff = quietCutoffEpochMillis(nowEpochMillis, timeZoneTodayKey)

        state.days.forEach { plan ->
            when (
                val decision = decideDigestSchedule(
                    dayKey = plan.dayKey,
                    fireAtEpochMillis = plan.fireAtEpochMillis,
                    nowEpochMillis = nowEpochMillis,
                    quietCutoffEpochMillis = quietCutoff,
                    deliveredDays = state.deliveredDays,
                    source = ReconcileSource.BACKGROUND,
                )
            ) {
                is DigestDecision.Schedule ->
                    // Catch-up decisions carry their own coalesced fire time;
                    // the stored plan's hour may already be elapsed (reboot
                    // after the digest hour).
                    scheduler.scheduleDay(plan.copy(fireAtEpochMillis = decision.fireAtEpochMillis))
                else -> Unit
            }
        }

        state.standalones.forEach { plan ->
            if (plan.key in state.deliveredKeys) return@forEach
            if (plan.fireAtEpochMillis > nowEpochMillis) {
                scheduler.scheduleStandalone(plan)
            } else if (nowEpochMillis <= quietCutoff) {
                scheduler.scheduleStandalone(
                    plan.copy(fireAtEpochMillis = nowEpochMillis + CatchUpDelayMillis),
                )
            }
        }
    }

    private fun quietCutoffEpochMillis(nowEpochMillis: Long, todayKey: Long): Long {
        val zone = TimeZone.currentSystemDefault()
        val day = LocalDate.fromEpochDays(todayKey)
        return runCatching {
            LocalDateTime(day.year, day.month, day.day, QuietCutoffHour, 0, 0)
                .toInstant(zone)
                .toEpochMilliseconds()
        }.getOrDefault(nowEpochMillis)
    }
}
