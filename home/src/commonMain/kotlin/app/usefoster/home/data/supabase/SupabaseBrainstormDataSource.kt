package app.usefoster.home.data.supabase

import app.usefoster.home.domain.BrainstormDataSource
import app.usefoster.home.domain.BrainstormError
import app.usefoster.home.domain.BrainstormGeneration
import app.usefoster.home.domain.BrainstormSession
import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.shared.domain.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

private val brainstormJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class TopicDto(
    val id: String? = null,
    val icon: String? = null,
    val title: String = "",
    val description: String? = null,
) {
    fun toDomain() = BrainstormTopic(id = id, icon = icon, title = title, description = description)
}

@Serializable
private data class BrainstormResponse(
    val success: Boolean = false,
    val cooldown: Boolean = false,
    val topics: List<TopicDto> = emptyList(),
)

@Serializable
private data class SessionDto(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("brainstorm_topics") val topics: List<TopicDto> = emptyList(),
) {
    fun toDomain() = BrainstormSession(id = id, createdAt = createdAt, topics = topics.map { it.toDomain() })
}

@Serializable
private data class SessionIdDto(val id: String)

/**
 * Generation runs server-side in the `brainstorm` Edge Function (it owns the
 * LLM key and the cooldown). History is read directly via RLS-safe PostgREST
 * selects (the `select_own` policies already scope rows to the caller).
 */
class SupabaseBrainstormDataSource(
    private val client: SupabaseClient,
) : BrainstormDataSource {

    override suspend fun generate(contactId: String): Result<BrainstormGeneration, BrainstormError> {
        return try {
            client.auth.currentSessionOrNull()
                ?: return Result.Error(BrainstormError.NotAuthenticated)

            // Non-2xx surfaces as a thrown RestException (caught below).
            val response: HttpResponse = client.functions.invoke("brainstorm") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("contactId", contactId) }.toString())
            }
            val body = response.bodyAsText()
            val parsed = brainstormJson.decodeFromString<BrainstormResponse>(body)

            when {
                parsed.cooldown -> Result.Success(BrainstormGeneration.Cooldown)
                parsed.success && parsed.topics.isNotEmpty() ->
                    Result.Success(BrainstormGeneration.Generated(parsed.topics.map { it.toDomain() }))
                else -> Result.Error(BrainstormError.Unknown("We couldn't generate suggestions. Please try again."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(mapError(e))
        }
    }

    override suspend fun getHistory(contactId: String): Result<List<BrainstormSession>, BrainstormError> {
        return try {
            client.auth.currentSessionOrNull()
                ?: return Result.Error(BrainstormError.NotAuthenticated)

            val sessions = client.postgrest
                .from("brainstorm_sessions")
                .select(Columns.raw("id, created_at, brainstorm_topics(id, icon, title, description)")) {
                    filter { eq("contact_id", contactId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<SessionDto>()
                .map { it.toDomain() }

            Result.Success(sessions)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(mapError(e))
        }
    }

    /**
     * Counts brainstorm sessions created this calendar month (UTC) for the
     * current user. RLS on `brainstorm_sessions` scopes rows to the caller's
     * contacts, so no explicit owner filter is needed.
     */
    override suspend fun getMonthlyGenerationCount(): Result<Int, BrainstormError> {
        return try {
            client.auth.currentSessionOrNull()
                ?: return Result.Error(BrainstormError.NotAuthenticated)

            // Instant.toString() is always ISO-8601 in UTC, e.g.
            // "2026-08-05T13:03:00Z" — so the first 8 chars are "YYYY-MM-".
            // Appending "01T00:00:00" yields this month's UTC start, matching the
            // UTC convention the brainstorm Edge Function already uses.
            val startOfMonth = Clock.System.now().toString().take(8) + "01T00:00:00"

            val count = client.postgrest
                .from("brainstorm_sessions")
                .select(Columns.raw("id")) {
                    filter { gte("created_at", startOfMonth) }
                }
                .decodeList<SessionIdDto>()
                .size

            Result.Success(count)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(mapError(e))
        }
    }

    private fun mapError(e: Exception): BrainstormError = when {
        e.message?.contains("JWT", ignoreCase = true) == true -> BrainstormError.NotAuthenticated
        e.message?.contains("401", ignoreCase = true) == true -> BrainstormError.NotAuthenticated
        e.message?.contains("Unauthorized", ignoreCase = true) == true -> BrainstormError.NotAuthenticated
        e.message?.contains("network", ignoreCase = true) == true -> BrainstormError.Network
        e.message?.contains("timeout", ignoreCase = true) == true -> BrainstormError.Network
        e.message?.contains("Connect", ignoreCase = true) == true -> BrainstormError.Network
        else -> BrainstormError.Unknown(e.message)
    }
}
