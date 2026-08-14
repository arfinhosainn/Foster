package app.usenekko.onboarding

import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.user.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthSessionStartupTest {

    @Test
    fun refreshFailureRequestsOneRecoveryAttemptWhileWelcomeIsVisible() {
        assertEquals(
            AuthSessionAction.Recover,
            authSessionAction(
                SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException())),
                isWelcome = true,
            ),
        )
    }

    @Test
    fun authenticatedSessionRoutesFromWelcome() {
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
                isWelcome = true,
            ),
        )
    }

    @Test
    fun sessionEventsAreIgnoredAfterLeavingWelcome() {
        assertEquals(
            AuthSessionAction.Ignore,
            authSessionAction(SessionStatus.Initializing, isWelcome = false),
        )
    }
}