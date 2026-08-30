package app.usefoster.home.presentation.contactprofile

fun formatContactFrequencyLabel(frequency: String): String = when (frequency.lowercase()) {
    "daily" -> "Daily"
    "weekly" -> "Weekly"
    "biweekly", "bi-weekly" -> "Bi-weekly"
    "monthly" -> "Monthly"
    "semi-annually", "semiannually" -> "Semi-annually"
    "annually", "annual" -> "Annually"
    else -> "No schedule"
}

fun formatContactReminderTime(time: String?): String {
    val parts = time?.trim()?.split(":") ?: return ""
    val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: return ""
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return ""
    if (hour24 !in 0..23 || minute !in 0..59) return ""

    val hour12 = (hour24 % 12).takeUnless { it == 0 } ?: 12
    val meridiem = if (hour24 < 12) "AM" else "PM"
    return "$hour12:${minute.toString().padStart(2, '0')}$meridiem"
}