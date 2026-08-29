package app.usenekko.onboarding

import app.usenekko.onboarding.domain.OnboardingAuthError
import app.usenekko.onboarding.domain.OnboardingProfileError
import app.usenekko.onboarding.domain.toOnboardingAuthError
import app.usenekko.onboarding.domain.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class OnboardingErrorMessageTest {

    @Test
    fun authErrorsUseSafeRetryMessages() {
        assertEquals(
            "Check your connection and try again",
            OnboardingAuthError.Network.toUserMessage(),
        )
        assertEquals(
            "Sign-in did not complete. Try again",
            OnboardingAuthError.Provider.toUserMessage(),
        )
        assertEquals(
            "Something went wrong. Please try again",
            OnboardingAuthError.Unexpected.toUserMessage(),
        )
    }

    @Test
    fun technicalProviderMessageIsClassifiedWithoutBeingDisplayed() {
        val error = IllegalStateException("OAuth provider returned HTTP 401 from SDK")

        assertEquals(OnboardingAuthError.Provider, error.toOnboardingAuthError())
        assertFalse(OnboardingAuthError.Provider.toUserMessage().contains("OAuth"))
        assertFalse(OnboardingAuthError.Provider.toUserMessage().contains("HTTP"))
        assertFalse(OnboardingAuthError.Provider.toUserMessage().contains("SDK"))
    }

    @Test
    fun networkAndUnexpectedExceptionsAreClassifiedSafely() {
        assertEquals(
            OnboardingAuthError.Network,
            IllegalStateException("connection timed out").toOnboardingAuthError(),
        )
        assertEquals(
            OnboardingAuthError.Unexpected,
            IllegalStateException("internal failure").toOnboardingAuthError(),
        )
    }

    @Test
    fun cancellationDoesNotBecomeAVisibleError() {
        assertNull(CancellationException("user cancelled").toOnboardingAuthError())
    }

    @Test
    fun unknownProfileDetailsAreNeverIncludedInUserMessage() {
        val message = OnboardingProfileError.Unknown("database: permission denied").toUserMessage()

        assertEquals("Something went wrong. Please try again", message)
        assertFalse(message.contains("database"))
        assertFalse(message.contains("permission denied"))
    }
}