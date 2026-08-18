package app.usenekko.onboarding

import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.user.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthSessionStartupTest {

    @Test
    fun refreshFailureRequestsOneRecoveryAttemptWhileSplashIsVisible() {
        assertEquals(
            AuthSessionAction.Recover,
            authSessionAction(
                SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException())),
                isSplash = true,
            ),
        )
    }

    @Test
    fun authenticatedSessionRoutesFromSplash() {
        assertEquals(
            AuthSessionAction.Route,
            authSessionAction(
                SessionStatus.Authenticated(
                    UserSession(
                        accessToken = "access-token",
                        refreshToken = "refresh-token",
                        expiresIn = 3600,
                        tokenType = "bearer",
                    ),
                ),
                isSplash = true,
            ),
        )
    }

    @Test
    fun unauthenticatedSessionShowsWelcomeAfterSplash() {
        assertEquals(
            AuthSessionAction.ShowWelcome,
            authSessionAction(SessionStatus.NotAuthenticated(), isSplash = true),
        )
    }

    @Test
    fun sessionEventsAreIgnoredAfterLeavingSplash() {
        assertEquals(
            AuthSessionAction.Ignore,
            authSessionAction(SessionStatus.Initializing, isSplash = false),
        )
    }
}