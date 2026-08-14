package app.usenekko.home.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class CheckIn(
    val id: String,
    val contactId: String,
    val checkedInAt: String,
    val note: String? = null,
)

fun CheckIn.localDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate? =
    runCatching { Instant.parse(checkedInAt).toLocalDateTime(timeZone).date }.getOrNull()

fun List<CheckIn>.contactIdsCheckedInOn(
    date: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Set<String> = filter { it.localDate(timeZone) == date }.mapTo(mutableSetOf()) { it.contactId }

data class CheckInUpdate(
    val lastCheckInDate: String,
    val nextCheckInDate: String?,
    val streakCount: Int,
)

/**
 * The full check-in cadence for a frequency, used when computing the next
 * check-in date after a check-in ([computeCheckInUpdate]). Keeping the interval
 * math in one place guarantees follow-up reminders use the exact cadence the
 * user picked.
 */
internal fun nextCheckInOffset(frequency: String): DatePeriod? = when (frequency) {
    "daily" -> DatePeriod(days = 1)
    "weekly" -> DatePeriod(days = 7)
    "biweekly" -> DatePeriod(days = 14)
    "monthly" -> DatePeriod(months = 1)
    else -> null
}

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

    return CheckInUpdate(
        lastCheckInDate = today.toString(),
        nextCheckInDate = nextCheckInOffset(current.checkInFrequency)?.let { today.plus(it).toString() },
        streakCount = streakCount,
    )
}
