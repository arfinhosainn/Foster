package app.usefoster.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
data class ReminderTimeDraft(
    val hour: Int,
    val minute: Int,
)
