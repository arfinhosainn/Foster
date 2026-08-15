package app.usenekko.home

import app.usenekko.shared.domain.AccountProfile
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.domain.ProfileError
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred

class FakeProfileDataSource(
    var profile: AccountProfile? = AccountProfile(
        fullName = "Jane Bell",
        displayName = null,
        avatarUrl = null,
        createdAt = "2026-01-15T10:00:00Z",
    ),
) : ProfileDataSource {
    var error: ProfileError? = null
    var getProfileCalls: Int = 0
        private set
    var updateSelectedAvatarIdCalls: Int = 0
        private set
    var getProfileGate: CompletableDeferred<Unit>? = null

    override suspend fun getProfile(): Result<AccountProfile, ProfileError> {
        getProfileCalls++
        getProfileGate?.await()
        error?.let { return Result.Error(it) }
        return profile?.let { Result.Success(it) }
            ?: Result.Error(ProfileError.Unknown("missing profile"))
    }

    override suspend fun updateSelectedAvatarId(selectedAvatarId: String): Result<Unit, ProfileError> {
        updateSelectedAvatarIdCalls++
        error?.let { return Result.Error(it) }
        profile = profile?.copy(selectedAvatarId = selectedAvatarId)
        return Result.Success(Unit)
    }
}