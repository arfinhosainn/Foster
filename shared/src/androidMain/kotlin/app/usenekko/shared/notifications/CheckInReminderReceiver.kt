package app.usenekko.shared.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Posts the check-in reminder notification when the [AlarmManager] alarm fires.
 * Registered in the app manifest; standalone, so it works when the app is
 * backgrounded or killed.
 */
class CheckInReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID) ?: return
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "your contact"

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Check in with $contactName")
            .setContentText("It's time to check in on $contactName")
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
            "Check-in reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders when it's time to check in with a contact"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}