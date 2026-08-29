package app.usenekko.onboarding.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompleteOnboardingPayload(
    val displayName: String? = null,
    val contactName: String? = null,
    val avatarUrl: String? = null,
    val selectedAvatarId: String? = null,
    val selectedAvatarColor: String? = null,
    val selectedGroupName: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val groups: List<GroupDto> = emptyList(),
    val reminderFrequency: String? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val customReminders: List<CustomReminderDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val notificationPermissionAsked: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
)

@Serializable
data class EnsureProfilePayload(
    val id: String,
    @SerialName("onboarding_step") val onboardingStep: Int,
)

@Serializable
data class GroupDto(
    val name: String,
    val color: String? = null,
)

@Serializable
data class CustomReminderDto(
    val title: String,
    val description: String = "",
    val recurrence: String = "none",
    val dateEpochMillis: Long? = null,
)

@Serializable
data class NoteDto(
    val title: String,
    val body: String = "",
)

@Serializable
data class OnboardingStepResponse(
    @SerialName("onboarding_step")
    val onboardingStep: Int?,
    @SerialName("onboarding_completed_at")
    val onboardingCompletedAt: String? = null,
)
