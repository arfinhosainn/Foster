package app.usenekko.shared.notifications

/**
 * Schedules grouped local notifications entirely on-device.
 *
 * No FCM/APNs remote push and no server-side component: every fire time is
 * already known in advance from local data, so this schedules local
 * alarms/notifications rather than receiving pushes.
 *
 * Model (docs/notification-system-plan.md):
 *  - [scheduleDay]: ONE grouped day-digest alarm per calendar day. All
 *    day-granular items due that day collapse into a single notification whose
 *    copy is baked into [DayPlan] at schedule time.
 *  - [scheduleStandalone]: time-specific items (custom reminders) fire at their
 *    own clock time and never fold into the digest.
 *  - [cancelDay]/[cancelStandalone] target alarms by identity; the reconciler
 *    drops days whose items were all cleared (never fires an empty digest).
 *
 * Platform behavior:
 *  - Android: `AlarmManager` (`setExactAndAllowWhileIdle`, with an inexact
 *    fallback when exact alarms are unavailable) →
 *    [CheckInReminderReceiver], which posts via NotificationManager into
 *    per-category channels and marks the day/key delivered.
 *  - iOS: `UNUserNotificationCenter` calendar triggers, request identifier =
 *    dayKey/plan key. iOS marks days delivered at schedule time (a scheduled
 *    iOS local notification always fires). iOS caps pending notifications at
 *    64 — see [MaxScheduledDays].
 *
 * Scheduling is a no-op when notifications are not authorized (checked
 * platform-side), so callers never branch on permission themselves.
 */
/**
 * Platform-agnostic scheduling surface ([ReminderScheduler] implements it on
 * both platforms). Exists so the reconciler can be exercised by a recording
 * fake in JVM unit tests instead of real AlarmManager / UNUserNotificationCenter.
 */
interface NotificationSchedulingOps {
    suspend fun scheduleDay(plan: DayPlan)
    suspend fun scheduleStandalone(plan: StandalonePlan)
    suspend fun cancelDay(dayKey: Long)
    suspend fun cancelStandalone(key: String)
}

expect class ReminderScheduler() : NotificationSchedulingOps {

    /** Whether the OS currently allows this app to post notifications. */
    suspend fun isEnabled(): Boolean

    /** Opens the OS notification settings page for this app. */
    suspend fun openSettings()
}

/**
 * Platform persistence for [NotificationPlanState] — the delivered/pending
 * sets and the full day-plan snapshot the Android boot/update/timezone
 * receiver uses to re-arm alarms without network or auth.
 */
expect fun defaultNotificationPlanStore(): NotificationPlanStore
