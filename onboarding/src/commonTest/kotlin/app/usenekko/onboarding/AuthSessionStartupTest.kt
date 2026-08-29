package app.usenekko.onboarding

import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.domain.OnboardingProfileError
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    @Test
    fun profileReadFailureStaysOnSplashAndDoesNotCreateOnboardingProfile() = runBlocking {
        val dataSource = RoutingProfileDataSource(
            onboardingStepResult = Result.Error(OnboardingProfileError.Network),
        )
        val navigator = Navigator(Screen.Splash)
        var reportedError: OnboardingProfileError? = null

        routeAfterAuth(dataSource, navigator) { reportedError = it }

        assertEquals(Screen.Splash, navigator.currentScreen)
        assertEquals(OnboardingProfileError.Network, reportedError)
        assertFalse(dataSource.ensureProfileCalled)
    }

    @Test
    fun missingProfileIsCreatedBeforeStartingOnboarding() = runBlocking {
        val dataSource = RoutingProfileDataSource(
            onboardingStepResult = Result.Error(OnboardingProfileError.ProfileNotFound),
            ensureResult = Result.Success(Unit),
        )
        val navigator = Navigator(Screen.Splash)

        routeAfterAuth(dataSource, navigator)

        assertEquals(Screen.Name, navigator.currentScreen)
        assertEquals(true, dataSource.ensureProfileCalled)
    }

    @Test
    fun profileCreationFailureStaysOnSplash() = runBlocking {
        val dataSource = RoutingProfileDataSource(
            onboardingStepResult = Result.Error(OnboardingProfileError.ProfileNotFound),
            ensureResult = Result.Error(OnboardingProfileError.Server),
        )
        val navigator = Navigator(Screen.Splash)
        var reportedError: OnboardingProfileError? = null

        routeAfterAuth(dataSource, navigator) { reportedError = it }

        assertEquals(Screen.Splash, navigator.currentScreen)
        assertEquals(OnboardingProfileError.Server, reportedError)
    }
}

private class RoutingProfileDataSource(
    private val onboardingStepResult: Result<OnboardingStep?, OnboardingProfileError>,
    private val ensureResult: EmptyResult<OnboardingProfileError> = Result.Success(Unit),
) : OnboardingProfileDataSource {
    var ensureProfileCalled = false
        private set

    override suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError> =
        Result.Success(Unit)

    override suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError> =
        onboardingStepResult

    override suspend fun ensureProfileExists(): EmptyResult<OnboardingProfileError> {
        ensureProfileCalled = true
        return ensureResult
    }
}