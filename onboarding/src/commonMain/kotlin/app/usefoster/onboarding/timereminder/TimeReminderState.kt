package app.usefoster.onboarding.timereminder

/**
 * State for the Time Reminder screen.
 *
 * @param selectedHour The currently selected hour (1–12).
 * @param selectedMinute The currently selected minute (0–59).
 * @param isAm Whether the selected period is AM (true) or PM (false).
 */
data class TimeReminderState(
    val selectedHour: Int = 10,
    val selectedMinute: Int = 30,
    val isAm: Boolean = false,
) {
    /** Formatted time string, e.g. "10:30". */
    val formattedTime: String
        get() = "${selectedHour}:${selectedMinute.toString().padStart(2, '0')}"

    /** Total selected minute-of-day in 12h range (used for scroll offset). */
    val totalMinutes: Int
        get() = selectedHour * 60 + selectedMinute
}
