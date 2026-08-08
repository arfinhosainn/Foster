package app.usenekko.home.domain

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
    val next = nextCheckInDateLocal() ?: return true
    return next <= today
}

fun Contact.isCheckedInToday(today: LocalDate): Boolean =
    lastCheckInDate == today.toString()

fun Contact.isDueOrCheckedInToday(today: LocalDate): Boolean =
    isOutstanding(today) || isCheckedInToday(today)

fun List<Contact>.forTodayCheckInList(today: LocalDate): List<Contact> =
    filter { it.isDueOrCheckedInToday(today) }
        .sortedBy { it.isCheckedInToday(today) }
