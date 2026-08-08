package app.usenekko.home.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class ContactReminder(
    val contactId: String,
    val contactName: String,
    val fireAtEpochMillis: Long,
)

/**
 * The moment this contact's reminder should fire: [date] at `reminder_time`, in
 * the device's time zone. Returns null when there is no reminder time or the
 * moment is already in the past (nothing worth scheduling). The fire time is
 * derived from existing columns — no server push involved.
 */
fun Contact.reminderAt(date: LocalDate, nowEpochMillis: Long): ContactReminder? {
    val fireAt = reminderEpochMillis(date) ?: return null

    return if (fireAt > nowEpochMillis) {
        ContactReminder(contactId = id, contactName = name, fireAtEpochMillis = fireAt)
    } else {
        null
    }
}

private fun Contact.reminderEpochMillis(date: LocalDate): Long? {
    val time = reminderTime ?: return null
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size < 2) return null
    val hour = parts[0].coerceIn(0, 23)
    val minute = parts[1].coerceIn(0, 59)
    val second = parts.getOrElse(2) { 0 }.coerceIn(0, 59)

    return runCatching {
        LocalDateTime(date.year, date.month, date.day, hour, minute, second)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }.getOrNull()
}

/**
 * Returns the scheduled check-in moment even when it has already passed today.
 * That lets the Home action show `Now` for overdue contacts instead of rolling
 * the displayed countdown to the following reminder.
 */
fun Contact.nextCheckInTargetEpochMillis(nowEpochMillis: Long): Long? =
    nextCheckInDateLocal()?.let { reminderEpochMillis(it) }
        ?: initialReminder(nowEpochMillis)?.fireAtEpochMillis

fun List<Contact>.nextUpcomingCheckInTargetEpochMillis(nowEpochMillis: Long): Long? =
    mapNotNull { it.nextCheckInTargetEpochMillis(nowEpochMillis) }
        .filter { it > nowEpochMillis }
        .minOrNull()

fun checkInCountdownLabel(remainingMillis: Long): String {
    if (remainingMillis <= 0L) return "Now"

    val totalSeconds = (remainingMillis + 999L) / 1_000L
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return when {
        days > 0L -> "${days}d ${hours}hr"
        hours > 0L -> if (minutes > 0L) "${hours}hr ${minutes}m" else "${hours}hr"
        minutes > 0L -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/**
 * Computes when this contact's next check-in reminder should fire locally:
 * `next_check_in_date` at `reminder_time`. Returns null when there is no next
 * check-in date (see [initialReminder] for brand-new contacts whose
 * `next_check_in_date` is still null), no reminder time, or the moment has
 * already passed.
 */
fun Contact.nextReminder(nowEpochMillis: Long): ContactReminder? {
    val date = nextCheckInDateLocal() ?: return null
    return reminderAt(date, nowEpochMillis)
}

/**
 * The first reminder for a brand-new contact: the next occurrence of
 * [reminderTime] strictly after [nowEpochMillis] — today if the time hasn't
 * passed yet, otherwise tomorrow. That's what lets contact created mid-day with
 * a "remind me at 07:30" setting fire at 07:30 the same day when still ahead,
 * and it makes fast on-device testing trivial (set a time a few minutes out and
 * it fires today).
 *
 * This is independent of cadence: it just finds the next time-of-day. Follow-up
 * reminders still drive off `next_check_in_date` via [nextReminder]. Returns
 * null when there is no reminder time.
 */
fun Contact.initialReminder(nowEpochMillis: Long): ContactReminder? {
    val time = reminderTime ?: return null
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size < 2) return null
    val hour = parts[0].coerceIn(0, 23)
    val minute = parts[1].coerceIn(0, 59)
    val second = parts.getOrElse(2) { 0 }.coerceIn(0, 59)

    val timeZone = TimeZone.currentSystemDefault()
    val nowLocal = Instant.fromEpochMilliseconds(nowEpochMillis).toLocalDateTime(timeZone)
    var candidate = LocalDateTime(
        nowLocal.date.year, nowLocal.date.month, nowLocal.date.day,
        hour, minute, second,
    )
    if (candidate.toInstant(timeZone).toEpochMilliseconds() <= nowEpochMillis) {
        val tomorrow = nowLocal.date.plus(DatePeriod(days = 1))
        candidate = LocalDateTime(
            tomorrow.year, tomorrow.month, tomorrow.day,
            hour, minute, second,
        )
    }
    val fireAt = candidate.toInstant(timeZone).toEpochMilliseconds()
    return ContactReminder(contactId = id, contactName = name, fireAtEpochMillis = fireAt)
}

/**
 * Selects which reminders to actually schedule. iOS enforces a hard cap of 64
 * pending notifications; when more than [app.usenekko.shared.notifications.MaxPendingReminders]
 * contacts have reminders we keep only the soonest ones (sorted by fire time),
 * which is exactly what the platform requires.
 */
fun List<ContactReminder>.takeSoonest(maxCount: Int = 64): List<ContactReminder> =
    sortedBy { it.fireAtEpochMillis }.take(maxCount)

/**
 * The full reminder plan for a set of contacts at a point in time, used by
 * Home's reconciliation.
 *
 * A checked-in contact drives off `next_check_in_date`; a brand-new contact
 * (whose `next_check_in_date` is still null) falls back to its initial reminder.
 * Without that fallback, reconciliation would cancel a fresh contact's
 * creation-time alarm and never re-schedule it — the first reminder would never
 * fire.
 */
fun List<Contact>.computeReminderPlans(nowEpochMillis: Long): List<ContactReminder> =
    mapNotNull { contact ->
        contact.nextReminder(nowEpochMillis) ?: contact.initialReminder(nowEpochMillis)
    }.takeSoonest()