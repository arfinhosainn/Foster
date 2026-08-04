package app.usenekko.shared.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android actual. The application context is supplied once at app start via
 * [ReminderScheduler.init]; before that (or if it was never called) all
 * operations are harmless no-ops.
 */
actual class ReminderScheduler {
    actual suspend fun schedule(
        contactId: String,
        contactName: String,
        fireAtEpochMillis: Long,
    ) {
        val context = ReminderScheduler.appContext ?: return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        withContext(Dispatchers.Main) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = fireAtEpochMillis

            if (triggerAt <= System.currentTimeMillis()) {
                // In the past — nothing to schedule.
                return@withContext
            }

            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent(context, contactId, contactName),
                )
            } else {
                // SCHEDULE_EXACT_ALARM denied — fall back to an inexact alarm rather
                // than crashing. Timing will drift slightly; exactness is best-effort.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent(context, contactId, contactName),
                )
            }
        }
    }

    actual suspend fun cancel(contactId: String) {
        val context = ReminderScheduler.appContext ?: return
        withContext(Dispatchers.Main) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent(context, contactId, contactName = null))
            // Drop any already-delivered notification for this contact too.
            NotificationManagerCompat.from(context).cancel(contactId.hashCode())
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

    private fun pendingIntent(
        context: Context,
        contactId: String,
        contactName: String?,
    ): PendingIntent {
        val intent = Intent(context, CheckInReminderReceiver::class.java).apply {
            putExtra(EXTRA_CONTACT_ID, contactId)
            putExtra(EXTRA_CONTACT_NAME, contactName)
        }
        // contactId (a UUID string) is the alarm request code so cancel() can
        // target the exact alarm for a contact.
        return PendingIntent.getBroadcast(
            context,
            contactId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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

internal const val EXTRA_CONTACT_ID = "extra_contact_id"
internal const val EXTRA_CONTACT_NAME = "extra_contact_name"
internal const val CHANNEL_ID = "check_in_reminders"