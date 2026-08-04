package app.usenekko.home

import app.usenekko.shared.domain.AccountProfile
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.domain.ProfileError
import app.usenekko.shared.domain.Result

class FakeProfileDataSource(
    var profile: AccountProfile? = AccountProfile(
        fullName = "Jane Bell",
        displayName = null,
        avatarUrl = null,
        createdAt = "2026-01-15T10:00:00Z",
    ),
) : ProfileDataSource {
    var error: ProfileError? = null

    override suspend fun getProfile(): Result<AccountProfile, ProfileError> {
        error?.let { return Result.Error(it) }
        return profile?.let { Result.Success(it) }
            ?: Result.Error(ProfileError.Unknown("missing profile"))
    }
}