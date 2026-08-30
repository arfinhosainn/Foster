package app.usefoster.onboarding.timereminder

/**
 * Sealed interface representing UI actions on the Time Reminder screen.
 */
sealed interface TimeReminderAction {
    /** User scrolled the dial to a new minute offset. */
    data class ScrollToMinute(val totalMinutes: Int) : TimeReminderAction

    /** User toggled between AM and PM. */
    data class ToggleAmPm(val isAm: Boolean) : TimeReminderAction
}
