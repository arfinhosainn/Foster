package app.usenekko.shared.notifications

/**
 * Schedules check-in reminder notifications entirely on-device.
 *
 * No FCM/APNs remote push and no server-side component: every reminder time is
 * already known in advance from `contacts.next_check_in_date` +
 * `contacts.reminder_time`, so this schedules one local alarm/notification per
 * contact rather than receiving pushes.
 *
 * Platform behavior:
 *  - Android: `AlarmManager` exact alarm (`setExactAndAllowWhileIdle`) with a
 *    [BroadcastReceiver][CheckInReminderReceiver] that posts via NotificationManager.
 *    Falls back to inexact scheduling if `SCHEDULE_EXACT_ALARM` is denied — never
 *    crashes. `contactId` is used as the alarm request code so [cancel] targets
 *    the right alarm.
 *  - iOS: `UNUserNotificationCenter` with a `UNCalendarNotificationTrigger`.
 *    `contactId` is the notification request identifier. iOS enforces a hard cap
 *    of 64 pending notifications — when more than [MaxPendingReminders] contacts
 *    have reminders, only the soonest [MaxPendingReminders] (sorted by fire time)
 *    are scheduled. See [MaxPendingReminders].
 *
 * [schedule]/[cancel] are no-ops if notifications are not authorized (checked
 * platform-side), so callers never need to branch on permission themselves.
 */
expect class ReminderScheduler() {
    suspend fun schedule(contactId: String, contactName: String, fireAtEpochMillis: Long)
    suspend fun cancel(contactId: String)

    /** Whether the OS currently allows this app to post notifications. */
    suspend fun isEnabled(): Boolean

    /** Opens the OS notification settings page for this app. */
    suspend fun openSettings()
}

/** iOS 64-pending-notification cap. Applied when reconciling schedules. */
const val MaxPendingReminders = 64