package app.usefoster.shared.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android actual. The application context is supplied once at app start via
 * [ReminderScheduler.init]; before that (or if it was never called) all
 * operations are harmless no-ops.
 *
 * Alarms use `setExactAndAllowWhileIdle` (fires during Doze). The
 * `USE_EXACT_ALARM` permission (Android 13+) is granted by default for
 * reminder-centric apps; `SCHEDULE_EXACT_ALARM` remains the fallback grant on
 * older targets, and an inexact alarm is the final fallback so scheduling
 * never crashes.
 */
actual class ReminderScheduler : NotificationSchedulingOps {
    actual override suspend fun scheduleDay(plan: DayPlan) {
        scheduleAlarm(
            requestCode = plan.dayKey.hashCode(),
            fireAtEpochMillis = plan.fireAtEpochMillis,
            buildIntent = { intent ->
                intent.putExtra(EXTRA_DAY_KEY, plan.dayKey)
                intent.putExtra(EXTRA_ITEM_COUNT, plan.itemCount)
                intent.putExtra(EXTRA_HEADLINE, plan.headline)
                intent.putExtra(EXTRA_CATEGORY, plan.category)
                plan.singleTargetId?.let { intent.putExtra(EXTRA_TARGET_ID, it) }
                intent.putExtra(EXTRA_TARGET_IS_CONTACT, plan.singleIsContact)
            },
        )
    }

    actual override suspend fun scheduleStandalone(plan: StandalonePlan) {
        scheduleAlarm(
            requestCode = plan.key.hashCode(),
            fireAtEpochMillis = plan.fireAtEpochMillis,
            buildIntent = { intent ->
                intent.putExtra(EXTRA_STANDALONE_KEY, plan.key)
                intent.putExtra(EXTRA_TITLE, plan.title)
                intent.putExtra(EXTRA_CATEGORY, plan.category)
                plan.targetId?.let { intent.putExtra(EXTRA_TARGET_ID, it) }
                intent.putExtra(EXTRA_TARGET_IS_CONTACT, plan.targetIsContact)
            },
        )
    }

    private suspend fun scheduleAlarm(
        requestCode: Int,
        fireAtEpochMillis: Long,
        buildIntent: (Intent) -> Unit,
    ) {
        val context = ReminderScheduler.appContext ?: return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        withContext(Dispatchers.Main) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (fireAtEpochMillis <= System.currentTimeMillis()) {
                // In the past — nothing to schedule. Elapsed-day policy (catch-up
                // or rollover) is decided by the reconciler, never here.
                return@withContext
            }

            val intent = Intent(context, CheckInReminderReceiver::class.java)
            buildIntent(intent)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fireAtEpochMillis,
                    pendingIntent,
                )
            } else {
                // Exact alarms unavailable — fall back to inexact rather than
                // crashing. Timing drifts slightly; delivery is best-effort.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fireAtEpochMillis,
                    pendingIntent,
                )
            }
        }
    }

    actual override suspend fun cancelDay(dayKey: Long) {
        val context = ReminderScheduler.appContext ?: return
        withContext(Dispatchers.Main) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmPendingIntent(context, dayKey.hashCode())?.let { alarmManager.cancel(it) }
            // Drop any already-delivered digest for this day too.
            NotificationManagerCompat.from(context).cancel(dayKey.hashCode())
        }
    }

    actual override suspend fun cancelStandalone(key: String) {
        val context = ReminderScheduler.appContext ?: return
        withContext(Dispatchers.Main) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            standalonePendingIntent(context, key)?.let { alarmManager.cancel(it) }
            NotificationManagerCompat.from(context).cancel(key.hashCode())
        }
    }

    actual suspend fun isEnabled(): Boolean {
        val context = ReminderScheduler.appContext ?: return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    actual suspend fun openSettings() {
        val context = ReminderScheduler.appContext ?: return
        withContext(Dispatchers.Main) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    private fun alarmPendingIntent(context: Context, requestCode: Int): PendingIntent? {
        // Extras are irrelevant for cancel matching; only the Intent filter
        // (component + action/data/type) and request code identify the alarm.
        // FLAG_NO_CREATE returns null when no matching alarm exists.
        val intent = Intent(context, CheckInReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun standalonePendingIntent(context: Context, key: String): PendingIntent? {
        val intent = Intent(context, CheckInReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Set once from the app entry point (MainActivity.onCreate). */
        var appContext: Context? = null
            private set

        fun init(context: Context) {
            appContext = context.applicationContext
        }
    }
}

internal const val EXTRA_DAY_KEY = "extra_day_key"
internal const val EXTRA_ITEM_COUNT = "extra_item_count"
internal const val EXTRA_HEADLINE = "extra_headline"
internal const val EXTRA_CATEGORY = "extra_category"
internal const val EXTRA_TARGET_ID = "extra_target_id"
internal const val EXTRA_TARGET_IS_CONTACT = "extra_target_is_contact"
internal const val EXTRA_STANDALONE_KEY = "extra_standalone_key"
internal const val EXTRA_TITLE = "extra_title"
internal const val CHANNEL_CHECK_INS = "check_in_reminders"
internal const val CHANNEL_CUSTOM_REMINDERS = "custom_reminders"
internal const val CHANNEL_MISSED_CHECK_INS = "missed_check_ins"
