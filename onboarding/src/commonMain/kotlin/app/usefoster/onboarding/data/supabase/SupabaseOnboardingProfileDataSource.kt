package app.usefoster.onboarding.data.supabase

import app.usefoster.onboarding.data.supabase.dto.CompleteOnboardingPayload
import app.usefoster.onboarding.data.supabase.dto.CustomReminderDto
import app.usefoster.onboarding.data.supabase.dto.EnsureProfilePayload
import app.usefoster.onboarding.data.supabase.dto.GroupDto
import app.usefoster.onboarding.data.supabase.dto.NoteDto
import app.usefoster.onboarding.data.supabase.dto.OnboardingStepResponse
import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingProfileDataSource
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.shared.domain.AccountProfile
import app.usefoster.shared.domain.ProfileDataSource
import app.usefoster.shared.domain.ProfileError
import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

private val payloadJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class ProfileDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("selected_avatar_id") val selectedAvatarId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

class SupabaseOnboardingProfileDataSource(
    private val client: SupabaseClient,
) : OnboardingProfileDataSource, ProfileDataSource {

    override suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(OnboardingProfileError.NotAuthenticated)

            val payload = draft.toCompleteOnboardingPayload(
                email = session.user?.email,
                emailVerified = session.user?.emailConfirmedAt != null,
            )

            val jsonElement = payloadJson.encodeToJsonElement(payload)

            val rpcParams = buildJsonObject {
                put("payload", jsonElement)
            }

            client.postgrest.rpc("complete_onboarding", rpcParams)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(OnboardingProfileError.NotAuthenticated)
            val userId = session.user?.id
                ?: return Result.Error(OnboardingProfileError.NotAuthenticated)

            val responses = client.postgrest
                .from("profiles")
                .select(columns = Columns.list("onboarding_step", "onboarding_completed_at")) {
                    filter { eq("id", userId) }
                }
                .decodeList<OnboardingStepResponse>()
            val response = when (responses.size) {
                0 -> return Result.Error(OnboardingProfileError.ProfileNotFound)
                1 -> responses.single()
                else -> return Result.Error(
                    OnboardingProfileError.Unknown("Multiple profiles found for the signed-in user"),
                )
            }

            val step = if (response.onboardingCompletedAt != null) {
                OnboardingStep.Complete
            } else {
                response.onboardingStep?.let { value ->
                    OnboardingStep.entries.firstOrNull { it.index == value }
                }
            }
            Result.Success(step)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getProfile(): Result<AccountProfile, ProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ProfileError.NotAuthenticated)

            val profile = client.postgrest
                .from("profiles")
                .select(
                    columns = Columns.list(
                        "full_name",
                        "display_name",
                        "avatar_url",
                        "selected_avatar_id",
                        "created_at",
                    ),
                ) {
                    single()
                }
                .decodeAs<ProfileDto>()

            Result.Success(
                AccountProfile(
                    fullName = profile.fullName,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    selectedAvatarId = profile.selectedAvatarId,
                    createdAt = profile.createdAt,
                )
            )
        } catch (e: Exception) {
            Result.Error(mapProfileError(e))
        }
    }

    override suspend fun updateSelectedAvatarId(selectedAvatarId: String): EmptyResult<ProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ProfileError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ProfileError.NotAuthenticated)

            client.postgrest
                .from("profiles")
                .update(mapOf("selected_avatar_id" to selectedAvatarId)) {
                    filter { eq("id", userId) }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapProfileError(e))
        }
    }

    override suspend fun ensureProfileExists(): EmptyResult<OnboardingProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(OnboardingProfileError.NotAuthenticated)

            val userId = session.user?.id ?: return Result.Error(OnboardingProfileError.NotAuthenticated)

            client.postgrest.from("profiles").upsert(
                EnsureProfilePayload(
                    id = userId,
                    onboardingStep = OnboardingStep.Name.index,
                ),
            ) {
                onConflict = "id"
                ignoreDuplicates = true
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    suspend fun uploadAvatar(
        bytes: ByteArray,
        extension: String,
    ): Result<String, OnboardingProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(OnboardingProfileError.NotAuthenticated)

            val userId = session.user?.id ?: return Result.Error(OnboardingProfileError.NotAuthenticated)
            val path = "avatars/$userId/profile.$extension"
            client.storage.from("avatars").upload(
                path = path,
                data = bytes,
            )
            val url = client.storage.from("avatars").publicUrl(path)
            Result.Success(url)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    private fun mapError(e: Exception): OnboardingProfileError {
        if (e is CancellationException) throw e
        return when {
            e.message?.contains("JWT", ignoreCase = true) == true -> OnboardingProfileError.NotAuthenticated
            e.message?.contains("network", ignoreCase = true) == true -> OnboardingProfileError.Network
            e.message?.contains("timeout", ignoreCase = true) == true -> OnboardingProfileError.Network
            else -> OnboardingProfileError.Unknown(detail = e.message)
        }
    }

    private fun mapProfileError(e: Exception): ProfileError {
        if (e is CancellationException) throw e
        return when {
            e.message?.contains("JWT", ignoreCase = true) == true -> ProfileError.NotAuthenticated
            e.message?.contains("network", ignoreCase = true) == true -> ProfileError.Network
            e.message?.contains("timeout", ignoreCase = true) == true -> ProfileError.Network
            else -> ProfileError.Unknown(detail = e.message)
        }
    }
}

internal fun OnboardingDraft.toCompleteOnboardingPayload(
    email: String?,
    emailVerified: Boolean,
): CompleteOnboardingPayload {
    val starterGroupNames = setOf("family", "friends")
    val onboardingGroups = buildList {
        add(GroupDto(name = "Family"))
        add(GroupDto(name = "Friends"))
        groups
            .filterNot { it.name.trim().lowercase() in starterGroupNames }
            .forEach { add(GroupDto(name = it.name, color = it.color)) }
    }

    return CompleteOnboardingPayload(
        displayName = name.ifEmpty { null },
        contactName = contactName.ifEmpty { null },
        avatarUrl = profilePhotoUri,
        selectedAvatarId = selectedAvatarId,
        selectedAvatarColor = selectedAvatarId.toOnboardingAvatarColor(),
        selectedGroupName = selectedGroupName(),
        email = email,
        emailVerified = emailVerified,
        groups = onboardingGroups,
        reminderFrequency = reminderFrequency?.name,
        reminderHour = reminderTime?.hour,
        reminderMinute = reminderTime?.minute,
        customReminders = customReminders.map {
            CustomReminderDto(
                title = it.title,
                description = it.description,
                recurrence = it.recurrence.name.lowercase(),
                dateEpochMillis = it.dateEpochMillis,
            )
        },
        notes = notes.map { NoteDto(title = it.title, body = it.body) },
        notificationPermissionAsked = notificationPermissionAsked,
        notificationPermissionGranted = notificationPermissionGranted,
    )
}

private fun OnboardingDraft.selectedGroupName(): String? = when (selectedGroupId) {
    "family" -> "Family"
    "friends" -> "Friends"
    else -> groups.firstOrNull { it.id == selectedGroupId }?.name
}

private fun String?.toOnboardingAvatarColor(): String? = when (this) {
    "0" -> "#FFCC33"
    "1" -> "#34C759"
    "2" -> "#FF9500"
    "3" -> "#FF3B30"
    "4" -> "#AF52DE"
    "5" -> "#007AFF"
    else -> null
}
