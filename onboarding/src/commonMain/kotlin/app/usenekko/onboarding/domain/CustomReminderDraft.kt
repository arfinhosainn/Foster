package app.usenekko.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
data class CustomReminderDraft(
    val id: String,
    val title: String,
    val description: String = "",
    val recurrence: ReminderFrequency = ReminderFrequency.None,
    val dateEpochMillis: Long? = null,
)
