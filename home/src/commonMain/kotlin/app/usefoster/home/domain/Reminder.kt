package app.usefoster.home.domain

data class Reminder(
    val id: String,
    val contactId: String,
    val title: String,
    val description: String,
    val recurrence: String,
    val dateEpochMillis: Long?,
    /**
     * Optional local clock time in 24h "HH:mm" form — the semantic deadline
     * for this reminder (plan §3.2 D1: "take medication at 8pm" fires at 8pm,
     * standalone, never folded into the digest). Null = date-only; occurrence
     * expansion then falls back to [CUSTOM_REMINDER_HOUR].
     */
    val timeOfDay: String? = null,
)

