package app.usenekko.home

import app.usenekko.home.domain.BrainstormError
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.DeleteAccountError
import app.usenekko.home.domain.toUserMessage
import app.usenekko.shared.domain.ProfileError
import app.usenekko.shared.subscription.SubscriptionError
import app.usenekko.shared.subscription.toUserMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeErrorMessagesTest {

    @Test
    fun contactErrorsUseSafeMessages() {
        assertEquals(
            "Check your connection and try again.",
            ContactError.Network.toUserMessage(),
        )
        assertEquals(
            "Something went wrong. Please try again.",
            ContactError.Unknown("HTTP 500: database failure").toUserMessage(),
        )
    }

    @Test
    fun homeErrorsNeverExposeTechnicalDetails() {
        val messages = listOf(
            BrainstormError.Unknown("SDK HTTP 500").toUserMessage(),
            ProfileError.Unknown("database permission denied").toUserMessage(),
            SubscriptionError.Unknown("store provider error").toUserMessage(),
        )

        messages.forEach { message ->
            assertEquals("Something went wrong. Please try again.", message)
            assertFalse(message.contains("HTTP"))
            assertFalse(message.contains("database"))
            assertFalse(message.contains("provider"))
        }
        assertEquals(
            "Something went wrong. Your account was not deleted.",
            DeleteAccountError.Unknown("server detail").toUserMessage(),
        )
    }
}