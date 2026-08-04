package app.usenekko.home.domain

import app.usenekko.shared.domain.Result

/**
 * Destructive account deletion. The real deletion runs server-side in a Supabase
 * Edge Function (`delete-account`) because removing the `auth.users` row needs
 * service-role privileges that a security-definer Postgres function cannot get.
 * Once the function returns success, the caller signs out locally and routes to
 * Welcome (there is no account left to route back to).
 */
interface DeleteAccountDataSource {
    suspend fun deleteAccount(): Result<Unit, DeleteAccountError>
}

sealed interface DeleteAccountError {
    data object NotAuthenticated : DeleteAccountError
    data object Network : DeleteAccountError
    data class Unknown(val detail: String?) : DeleteAccountError
}
