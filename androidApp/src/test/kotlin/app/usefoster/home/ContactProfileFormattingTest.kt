package app.usefoster.home

import app.usefoster.home.presentation.components.avatarIndexForColor
import app.usefoster.home.presentation.contactprofile.formatContactFrequencyLabel
import app.usefoster.home.presentation.contactprofile.formatContactReminderTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactProfileFormattingTest {
    @Test
    fun formatsFrequencyWithRequestedCapitalization() {
        assertEquals("Bi-weekly", formatContactFrequencyLabel("biweekly"))
        assertEquals("Semi-annually", formatContactFrequencyLabel("semi-annually"))
        assertEquals("No schedule", formatContactFrequencyLabel("none"))
    }

    @Test
    fun formatsTwentyFourHourTimeAsCompactTwelveHourTime() {
        assertEquals("7:30AM", formatContactReminderTime("07:30:00"))
        assertEquals("12:00PM", formatContactReminderTime("12:00:00"))
        assertEquals("12:05AM", formatContactReminderTime("00:05:00"))
        assertEquals("6:45PM", formatContactReminderTime("18:45"))
    }

    @Test
    fun invalidReminderTimeDoesNotRenderMisleadingScheduleText() {
        assertEquals("", formatContactReminderTime(null))
        assertEquals("", formatContactReminderTime("25:70:00"))
        assertEquals("", formatContactReminderTime("not-a-time"))
    }

    @Test
    fun contactAvatarMappingUsesTheSavedContactColor() {
        assertEquals(0, avatarIndexForColor("#FFCC33"))
        assertEquals(5, avatarIndexForColor("#007AFF"))
    }
}