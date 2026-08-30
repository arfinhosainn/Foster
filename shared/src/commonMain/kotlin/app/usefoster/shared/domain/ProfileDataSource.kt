package app.usefoster.shared.domain

/**
 * Owner-scoped read of the authenticated user's own profile row.
 *
 * Lives in `shared` (next to [Result]) so both feature modules — which must not
 * depend on one another — can reference it. The ownership convention matches the
 * `ContactDataSource` pattern (every query scoped by `auth.uid()` / the session id),
 * and the `profiles` read is reused from onboarding (see `getOnboardingStep`),
 * never duplicated.
 */
interface ProfileDataSource {
    suspend fun getProfile(): Result<AccountProfile, ProfileError>
    suspend fun updateSelectedAvatarId(selectedAvatarId: String): EmptyResult<ProfileError>
}

data class AccountProfile(
    val fullName: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val selectedAvatarId: String? = null,
    val createdAt: String?,
) {
    /** Prefer the ERD-aligned full name, falling back to the onboarding display name. */
    val resolvedName: String?
        get() = fullName?.takeIf { it.isNotBlank() } ?: displayName?.takeIf { it.isNotBlank() }
}

sealed interface ProfileError {
    data object NotAuthenticated : ProfileError
    data object Network : ProfileError
    data class Unknown(val detail: String?) : ProfileError
}