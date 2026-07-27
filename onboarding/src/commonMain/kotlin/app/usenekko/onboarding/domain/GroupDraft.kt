package app.usenekko.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
data class GroupDraft(
    val id: String,
    val name: String,
    val color: String? = null,
)
