package app.usenekko.shared.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.usenekko.shared.R

/**
 * Posts the check-in reminder notification when the [AlarmManager] alarm fires.
 * Registered in the app manifest; standalone, so it works when the app is
 * backgrounded or killed.
 */
class CheckInReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID) ?: return
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
            ?: context.getString(R.string.notif_contact_fallback)

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        createChannel(context)

        // Resolved at render time from resources so the notification follows
        // the device language (and Android 13+ per-app language settings).
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_checkin_title, contactName))
            .setContentText(context.getString(R.string.notif_checkin_body, contactName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Same request code as the alarm so cancel() clears this too.
        NotificationManagerCompat.from(context).notify(contactId.hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}