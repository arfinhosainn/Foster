package app.usenekko.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingDraft(
    val name: String = "",
    val contactName: String = "",
    val profilePhotoUri: String? = null,
    val selectedAvatarId: String? = null,
    val selectedGroupId: String? = null,
    val groups: List<GroupDraft> = emptyList(),
    val reminderFrequency: ReminderFrequency? = null,
    val reminderTime: ReminderTimeDraft? = null,
    val customReminders: List<CustomReminderDraft> = emptyList(),
    val notes: List<NoteDraft> = emptyList(),
    val notificationPermissionAsked: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val lastUpdatedAtMillis: Long = 0L,
)
