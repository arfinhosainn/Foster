package app.usefoster.shared.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.usefoster.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Posts notification alarms: the grouped day digest and standalone custom
 * reminders. Registered in the app manifest; standalone, so it works when the
 * app is backgrounded or killed.
 *
 * Copy is resolved at render time from resources (plurals included) so the
 * notification follows the device language. Posting marks the day/key
 * delivered in the plan store so reconciles never double-buzz (plan §3.3).
 *
 * Tapping opens MainActivity, which routes to the day agenda (digests) or the
 * contact profile (single contact / custom reminder with a contact).
 */
class CheckInReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val pendingResult = goAsync()
        val category = intent.getStringExtra(EXTRA_CATEGORY)
            ?: NotificationCategories.CHECK_INS
        createChannel(context, category)

        val notification = when {
            intent.hasExtra(EXTRA_DAY_KEY) -> {
                val dayKey = intent.getLongExtra(EXTRA_DAY_KEY, 0L)
                val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 1)
                val headline = intent.getStringExtra(EXTRA_HEADLINE)
                    ?: context.getString(R.string.notif_contact_fallback)
                val targetId = intent.getStringExtra(EXTRA_TARGET_ID)

                buildDigestNotification(context, itemCount, headline, category, dayKey, targetId)
                    .also { markDayDelivered(dayKey, pendingResult) }
            }

            intent.hasExtra(EXTRA_STANDALONE_KEY) -> {
                val key = intent.getStringExtra(EXTRA_STANDALONE_KEY) ?: return run { pendingResult.finish() }
                val title = intent.getStringExtra(EXTRA_TITLE)
                    ?: context.getString(R.string.notif_contact_fallback)
                val targetId = intent.getStringExtra(EXTRA_TARGET_ID)
                val targetIsContact = intent.getBooleanExtra(EXTRA_TARGET_IS_CONTACT, false)

                buildStandaloneNotification(context, title, category, key, targetId, targetIsContact)
                    .also { markKeyDelivered(key, pendingResult) }
            }

            else -> {
                pendingResult.finish()
                return
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId(intent), notification)
    }

    private fun notificationId(intent: Intent): Int =
        if (intent.hasExtra(EXTRA_DAY_KEY)) {
            intent.getLongExtra(EXTRA_DAY_KEY, 0L).hashCode()
        } else {
            (intent.getStringExtra(EXTRA_STANDALONE_KEY) ?: "").hashCode()
        }

    private fun buildDigestNotification(
        context: Context,
        itemCount: Int,
        headline: String,
        category: String,
        dayKey: Long,
        targetId: String?,
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(context, channelFor(category))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, dayKey = dayKey, contactId = null))

        if (itemCount <= 1) {
            builder.setContentTitle(context.getString(R.string.notif_checkin_title, headline))
            builder.setContentText(context.getString(R.string.notif_checkin_body, headline))
        } else {
            builder.setContentTitle(
                context.resources.getQuantityString(
                    R.plurals.notif_digest_title,
                    itemCount,
                    itemCount,
                ),
            )
            builder.setContentText(
                context.getString(R.string.notif_digest_body, headline, itemCount - 1),
            )
        }
        return builder.build()
    }

    private fun buildStandaloneNotification(
        context: Context,
        title: String,
        category: String,
        key: String,
        targetId: String?,
        targetIsContact: Boolean,
    ): android.app.Notification {
        return NotificationCompat.Builder(context, channelFor(category))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                contentIntent(
                    context,
                    dayKey = null,
                    contactId = if (targetIsContact) targetId else null,
                ),
            )
            .setContentTitle(context.getString(R.string.notif_custom_reminder_title, title))
            .setContentText(context.getString(R.string.notif_custom_reminder_body))
            .build()
    }

    private fun contentIntent(
        context: Context,
        dayKey: Long?,
        contactId: String?,
    ): PendingIntent {
        val intent = Intent(context, notificationTapActivity()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(NotificationTapExtras.EXTRA_OPEN_DAY_KEY, dayKey)
            putExtra(NotificationTapExtras.EXTRA_OPEN_CONTACT_ID, contactId)
        }
        val requestCode = (dayKey ?: contactId?.hashCode() ?: 0).hashCode()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationTapActivity(): Class<*> =
        Class.forName("app.usefoster.MainActivity")

    private fun markDayDelivered(dayKey: Long, pendingResult: PendingResult) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            runCatching {
                val store = defaultNotificationPlanStore()
                val state = store.load()
                store.save(
                    state.copy(deliveredDays = state.deliveredDays + dayKey),
                )
            }
            pendingResult.finish()
        }
    }

    private fun markKeyDelivered(key: String, pendingResult: PendingResult) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            runCatching {
                val store = defaultNotificationPlanStore()
                val state = store.load()
                store.save(state.copy(deliveredKeys = state.deliveredKeys + key))
            }
            pendingResult.finish()
        }
    }

    companion object {
        fun channelFor(category: String): String = when (category) {
            NotificationCategories.CUSTOM_REMINDERS -> CHANNEL_CUSTOM_REMINDERS
            NotificationCategories.MISSED_CHECK_INS -> CHANNEL_MISSED_CHECK_INS
            else -> CHANNEL_CHECK_INS
        }

        fun createChannel(context: Context, category: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val (id, nameRes, descRes) = when (channelFor(category)) {
                CHANNEL_CUSTOM_REMINDERS -> Triple(
                    CHANNEL_CUSTOM_REMINDERS,
                    R.string.notif_channel_custom_reminders,
                    R.string.notif_channel_custom_reminders_description,
                )

                CHANNEL_MISSED_CHECK_INS -> Triple(
                    CHANNEL_MISSED_CHECK_INS,
                    R.string.notif_channel_missed_check_ins,
                    R.string.notif_channel_missed_check_ins_description,
                )

                else -> Triple(
                    CHANNEL_CHECK_INS,
                    R.string.notif_channel_name,
                    R.string.notif_channel_description,
                )
            }
            val channel = NotificationChannel(
                id,
                context.getString(nameRes),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(descRes)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}

/** Extras MainActivity reads to route notification taps (cold start included). */
object NotificationTapExtras {
    const val EXTRA_OPEN_DAY_KEY = "extra_open_day_key"
    const val EXTRA_OPEN_CONTACT_ID = "extra_open_contact_id"
}
