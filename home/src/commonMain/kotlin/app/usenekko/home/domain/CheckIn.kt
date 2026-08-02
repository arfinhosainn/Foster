package app.usenekko.home.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class CheckIn(
    val id: String,
    val contactId: String,
    val checkedInAt: String,
    val note: String? = null,
)

data class CheckInUpdate(
    val lastCheckInDate: String,
    val nextCheckInDate: String?,
    val streakCount: Int,
)

/**
 * Computes the new denormalized check-in state for a contact after a check-in
 * on [today]. `contacts.last_check_in_date / next_check_in_date / streak_count`
 * have no DB trigger (see migration_v2_erd.sql), so the app must update them on
 * every check-in or they drift.
 */
fun computeCheckInUpdate(current: Contact, today: LocalDate): CheckInUpdate {
    val windowDays = when (current.checkInFrequency) {
        "daily" -> 1
        "weekly" -> 7
        "biweekly" -> 14
        else -> 30
    }

    val streakCount = current.lastCheckInDate
        ?.let { last -> runCatching { LocalDate.parse(last) }.getOrNull() }
        ?.let { last ->
            if (last == today) {
                current.streakCount
            } else if (today.toEpochDays() - last.toEpochDays() <= windowDays) {
                current.streakCount + 1
            } else {
                1
            }
        }
        ?: 1

    val nextCheckInDate = when (current.checkInFrequency) {
        "daily" -> today.plus(DatePeriod(days = 1))
        "weekly" -> today.plus(DatePeriod(days = 7))
        "biweekly" -> today.plus(DatePeriod(days = 14))
        "monthly" -> today.plus(DatePeriod(months = 1))
        else -> null
    }

    return CheckInUpdate(
        lastCheckInDate = today.toString(),
        nextCheckInDate = nextCheckInDate?.toString(),
        streakCount = streakCount,
    )
}
