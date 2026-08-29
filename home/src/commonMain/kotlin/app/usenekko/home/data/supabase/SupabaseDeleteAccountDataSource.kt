package app.usenekko.home.data.supabase

import app.usenekko.home.domain.DeleteAccountDataSource
import app.usenekko.home.domain.DeleteAccountError
import app.usenekko.shared.domain.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException

private val deleteJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class DeleteAccountResponse(
    val success: Boolean = false,
    val error: String? = null,
)

/**
 * Calls the `delete-account` Edge Function with the caller's own JWT (sent
 * automatically by the Functions plugin). The function verifies the session,
 * removes the avatar storage folder, then deletes the `auth.users` row via the
 * service role — which cascades to every public table (verified `confdeltype =
 * 'c'` end-to-end).
 *
 * Non-2xx responses (e.g. 401 invalid session, 500 delete failure) surface as a
 * thrown [io.github.jan.supabase.exceptions.RestException] and are mapped to an
 * error. Success is only reported when the function returns `{success:true}` —
 * the caller must not sign the user out otherwise.
 */
class SupabaseDeleteAccountDataSource(
    private val client: SupabaseClient,
) : DeleteAccountDataSource {

    override suspend fun deleteAccount(): Result<Unit, DeleteAccountError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(DeleteAccountError.NotAuthenticated)

            val response: HttpResponse = client.functions.invoke("delete-account") { }
            val body = response.bodyAsText()
            val parsed = try {
                deleteJson.decodeFromString<DeleteAccountResponse>(body)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                DeleteAccountResponse(success = response.status.isSuccess())
            }

            if (parsed.success) {
                Result.Success(Unit)
            } else {
                val detail = parsed.error?.takeIf { it.isNotBlank() }
                    ?: "The server could not delete your account."
                Result.Error(DeleteAccountError.Unknown(detail))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(mapError(e))
        }
    }

    private fun mapError(e: Exception): DeleteAccountError = when {
        e.message?.contains("JWT", ignoreCase = true) == true -> DeleteAccountError.NotAuthenticated
        e.message?.contains("401", ignoreCase = true) == true -> DeleteAccountError.NotAuthenticated
        e.message?.contains("Unauthorized", ignoreCase = true) == true -> DeleteAccountError.NotAuthenticated
        e.message?.contains("network", ignoreCase = true) == true -> DeleteAccountError.Network
        e.message?.contains("timeout", ignoreCase = true) == true -> DeleteAccountError.Network
        e.message?.contains("Connect", ignoreCase = true) == true -> DeleteAccountError.Network
        else -> DeleteAccountError.Unknown(e.message)
    }
}
