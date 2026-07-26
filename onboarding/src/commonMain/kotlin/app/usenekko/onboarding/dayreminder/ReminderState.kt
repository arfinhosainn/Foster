package app.usenekko.onboarding.dayreminder

/**
 * Data class holding the state for the Reminder screen.
 * @param selectedOption The currently selected reminder frequency.
 */
data class ReminderState(
    val selectedOption: String = ReminderOptions.DAILY
)

/**
 * Available frequency options for the reminder screen.
 */
object ReminderOptions {
    const val DAILY = "Daily"
    const val WEEKLY = "Weekly"
    const val BI_WEEKLY = "Bi-weekly"
    const val MONTHLY = "Monthly"
    const val SEMI_ANNUALLY = "Semi-annually"
    const val ANNUALLY = "Annually"

    val all = listOf(DAILY, WEEKLY, BI_WEEKLY, MONTHLY, SEMI_ANNUALLY, ANNUALLY)
}
