package app.usefoster.home.domain

import kotlinx.datetime.LocalDate

data class Contact(
    val id: String,
    val name: String,
    val avatarColor: String?,
    val checkInFrequency: String,
    val reminderTime: String?,
    val nextCheckInDate: String?,
    val lastCheckInDate: String?,
    val streakCount: Int,
)

fun Contact.nextCheckInDateLocal(): LocalDate? =
    nextCheckInDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun Contact.isOutstanding(today: LocalDate): Boolean {
    val next = nextCheckInDateLocal() ?: return !isCheckedInToday(today)
    return next <= today
}

fun Contact.isCheckedInToday(today: LocalDate): Boolean =
    lastCheckInDate == today.toString()

private val CADENCE_INTERVAL_DAYS = mapOf(
    "daily" to 1,
    "weekly" to 7,
    "biweekly" to 14,
    "monthly" to 30,
    "semiannually" to 182,
    "annually" to 365,
)

fun Contact.checkInProgressFraction(today: LocalDate): Float {
    val intervalDays = CADENCE_INTERVAL_DAYS[checkInFrequency.lowercase()] ?: return 0f
    val next = nextCheckInDateLocal() ?: return 0f
    val daysLeft = (next.toEpochDays() - today.toEpochDays()).toFloat()
    return (1f - daysLeft / intervalDays).coerceIn(0f, 1f)
}

fun Contact.isDueOrCheckedInToday(today: LocalDate): Boolean =
    isOutstanding(today) || isCheckedInToday(today)

fun List<Contact>.forTodayCheckInList(today: LocalDate): List<Contact> =
    filter { it.isDueOrCheckedInToday(today) }
        .sortedBy { it.isCheckedInToday(today) }
