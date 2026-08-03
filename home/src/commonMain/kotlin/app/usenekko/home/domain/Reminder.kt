package app.usenekko.home.domain

data class Reminder(
    val id: String,
    val contactId: String,
    val title: String,
    val description: String,
    val recurrence: String,
    val dateEpochMillis: Long?,
)
