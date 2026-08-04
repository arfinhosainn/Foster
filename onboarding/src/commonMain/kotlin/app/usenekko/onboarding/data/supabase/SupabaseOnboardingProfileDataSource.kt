package app.usenekko.onboarding.data.supabase

import app.usenekko.onboarding.data.supabase.dto.CompleteOnboardingPayload
import app.usenekko.onboarding.data.supabase.dto.CustomReminderDto
import app.usenekko.onboarding.data.supabase.dto.GroupDto
import app.usenekko.onboarding.data.supabase.dto.NoteDto
import app.usenekko.onboarding.data.supabase.dto.OnboardingStepResponse
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.domain.OnboardingProfileError
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.shared.domain.AccountProfile
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.domain.ProfileError
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
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
    @SerialName("created_at") val createdAt: String? = null,
)

class SupabaseOnboardingProfileDataSource(
    private val client: SupabaseClient,
) : OnboardingProfileDataSource, ProfileDataSource {

    override suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(OnboardingProfileError.NotAuthenticated)

            val payload = CompleteOnboardingPayload(
                displayName = draft.name.ifEmpty { null },
                contactName = draft.contactName.ifEmpty { null },
                avatarUrl = draft.profilePhotoUri,
                selectedAvatarId = draft.selectedAvatarId,
                email = session.user?.email,
                emailVerified = session.user?.emailConfirmedAt != null,
                groups = draft.groups.map { GroupDto(name = it.name, color = it.color) },
                reminderFrequency = draft.reminderFrequency?.name,
                reminderHour = draft.reminderTime?.hour,
                reminderMinute = draft.reminderTime?.minute,
                customReminders = draft.customReminders.map {
                    CustomReminderDto(
                        title = it.title,
                        description = it.description,
                        recurrence = it.recurrence.name.lowercase(),
                        dateEpochMillis = it.dateEpochMillis,
                    )
                },
                notes = draft.notes.map { NoteDto(title = it.title, body = it.body) },
                notificationPermissionAsked = draft.notificationPermissionAsked,
                notificationPermissionGranted = draft.notificationPermissionGranted,
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

            val response = client.postgrest
                .from("profiles")
                .select(columns = Columns.list("onboarding_step", "onboarding_completed_at")) {
                    single()
                }
                .decodeAs<OnboardingStepResponse>()

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
                .select(columns = Columns.list("full_name", "display_name", "avatar_url", "created_at")) {
                    single()
                }
                .decodeAs<ProfileDto>()

            Result.Success(
                AccountProfile(
                    fullName = profile.fullName,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    createdAt = profile.createdAt,
                )
            )
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
                mapOf("id" to userId, "onboarding_step" to OnboardingStep.Name.index)
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
        return when {
            e.message?.contains("JWT", ignoreCase = true) == true -> OnboardingProfileError.NotAuthenticated
            e.message?.contains("network", ignoreCase = true) == true -> OnboardingProfileError.Network
            e.message?.contains("timeout", ignoreCase = true) == true -> OnboardingProfileError.Network
            else -> OnboardingProfileError.Unknown(detail = e.message)
        }
    }

    private fun mapProfileError(e: Exception): ProfileError {
        return when {
            e.message?.contains("JWT", ignoreCase = true) == true -> ProfileError.NotAuthenticated
            e.message?.contains("network", ignoreCase = true) == true -> ProfileError.Network
            e.message?.contains("timeout", ignoreCase = true) == true -> ProfileError.Network
            else -> ProfileError.Unknown(detail = e.message)
        }
    }
}
