package app.usefoster.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderFrequency {
    Daily,
    Weekly,
    BiWeekly,
    Monthly,
    SemiAnnually,
    Annually,
    None,
}
