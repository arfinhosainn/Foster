package app.usenekko.onboarding.reminder

/**
 * Sealed interface representing UI actions in the Reminder screen.
 */
sealed interface ReminderAction {
    /**
     * Triggered when the user selects a new frequency option.
     */
    data class SelectOption(val option: String) : ReminderAction
}
