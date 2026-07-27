package app.usenekko.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
data class NoteDraft(
    val id: String,
    val title: String,
    val body: String = "",
)
