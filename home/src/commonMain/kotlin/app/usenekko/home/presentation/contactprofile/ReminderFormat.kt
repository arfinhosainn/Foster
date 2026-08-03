package app.usenekko.home.presentation.contactprofile

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val reminderRecurrenceOptions = listOf(
    "None",
    "Daily",
    "Weekly",
    "Bi-weekly",
    "Monthly",
    "Semi-annually",
    "Yearly",
)

fun recurrenceToDbValue(label: String): String = when (label) {
    "Daily" -> "daily"
    "Weekly" -> "weekly"
    "Bi-weekly" -> "biweekly"
    "Monthly" -> "monthly"
    "Semi-annually" -> "semiannually"
    "Yearly" -> "annually"
    else -> "none"
}

fun recurrenceToUiLabel(dbValue: String): String = when (dbValue) {
    "daily" -> "Daily"
    "weekly" -> "Weekly"
    "biweekly" -> "Bi-weekly"
    "monthly" -> "Monthly"
    "semiannually" -> "Semi-annually"
    "annually" -> "Yearly"
    else -> "None"
}

fun formatReminderDate(dateEpochMillis: Long?): String {
    if (dateEpochMillis == null) return "Choose Date"
    val date = Instant.fromEpochMilliseconds(dateEpochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return formatMonthDayYear(date)
}

fun formatCheckInDate(date: String?): String {
    if (date == null) return "No upcoming"
    return runCatching { LocalDate.parse(date) }
        .getOrNull()
        ?.let { formatMonthDayYear(it) }
        ?: "No upcoming"
}

private fun formatMonthDayYear(date: LocalDate): String {
    val monthAbbr = date.month.name.lowercase()
        .replaceFirstChar { it.uppercaseChar() }
        .take(3)
    return "$monthAbbr ${date.day}, ${date.year}"
}
