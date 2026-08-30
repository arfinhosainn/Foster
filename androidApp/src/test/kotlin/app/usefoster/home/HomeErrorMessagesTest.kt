package app.usefoster.home

import app.usefoster.home.domain.BrainstormError
import app.usefoster.home.domain.ContactError
import app.usefoster.home.domain.DeleteAccountError
import app.usefoster.home.domain.toUserMessage
import app.usefoster.shared.domain.ProfileError
import app.usefoster.shared.subscription.SubscriptionError
import app.usefoster.shared.subscription.toUserMessage
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