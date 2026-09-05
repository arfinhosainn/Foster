package app.usefoster.home

import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.home.presentation.brainstorm.formatTopicMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BrainstormMessageFormatterTest {

    private val topic = BrainstormTopic(
        id = "t1",
        icon = "sparkle",
        title = "Catch up",
        description = "Ask what's new with them and what they've been up to lately.",
    )

    @Test
    fun formatsDescriptionAsPlainText() {
        assertEquals(
            "Ask what's new with them and what they've been up to lately.",
            formatTopicMessage(topic),
        )
    }

    @Test
    fun blankDescriptionYieldsEmptyString() {
        assertEquals("", formatTopicMessage(topic.copy(description = null)))
        assertEquals("", formatTopicMessage(topic.copy(description = "   ")))
    }

    @Test
    fun titleIsNeverIncludedInMessage() {
        val titledOnly = topic.copy(description = "Body text")
        assertEquals("Body text", formatTopicMessage(titledOnly))
        assertFalse(formatTopicMessage(titledOnly).contains("Catch up"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals(
            "Body",
            formatTopicMessage(BrainstormTopic(title = "  Title  ", description = "  Body  ")),
        )
    }

    @Test
    fun doesNotAppendSignatureOrDebugChrome() {
        val message = formatTopicMessage(topic)
        assertFalse(message.contains("Sent from Foster", ignoreCase = true))
        assertFalse(message.contains("{", ignoreCase = true))
        assertFalse(message.contains("}", ignoreCase = true))
    }
}