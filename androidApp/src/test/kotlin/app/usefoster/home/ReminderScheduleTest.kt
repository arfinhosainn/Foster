package app.usefoster.home

import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.checkInCountdownLabel
import app.usefoster.home.domain.nextCheckInTargetEpochMillis
import app.usefoster.home.domain.nextUpcomingCheckInTargetEpochMillis
import app.usefoster.home.domain.initialReminder
import app.usefoster.home.domain.nextReminder
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderScheduleTest {

    private val timeZone = TimeZone.currentSystemDefault()

    private fun contact(reminderTime: String? = "07:30:00") = Contact(
        id = "c1",
        name = "Arfin",
        avatarColor = "#007AFF",
        checkInFrequency = "daily",
        reminderTime = reminderTime,
        nextCheckInDate = null,
        lastCheckInDate = null,
        streakCount = 0,
    )

    /** Builds an epoch millis for a local date/time in the device zone. */
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int): Long =
        LocalDateTime(y, mo, d, h, mi, s).toInstant(timeZone).toEpochMilliseconds()

    @Test
    fun firstReminderFiresTodayWhenTimeStillAhead() {
        // Now = 12:00, reminder 07:30 -> already passed today -> tomorrow 07:30.
        val reminder = contact().initialReminder(at(2026, 8, 4, 12, 0, 0))
        assertNotNull(reminder)
        assertEquals(at(2026, 8, 5, 7, 30, 0), reminder!!.fireAtEpochMillis)
        assertEquals("c1", reminder.contactId)
    }

    @Test
    fun firstReminderFiresTodayWhenTimeNotYetPassed() {
        // Now = 06:00, reminder 07:30 -> later today -> today 07:30.
        val reminder = contact().initialReminder(at(2026, 8, 4, 6, 0, 0))
        assertNotNull(reminder)
        assertEquals(at(2026, 8, 4, 7, 30, 0), reminder!!.fireAtEpochMillis)
    }

    @Test
    fun noReminderTimeSchedulesNothing() {
        assertNull(contact(reminderTime = null).initialReminder(at(2026, 8, 4, 6, 0, 0)))
    }

    @Test
    fun malformedReminderTimeSchedulesNothing() {
        assertNull(contact(reminderTime = "not-a-time").initialReminder(at(2026, 8, 4, 6, 0, 0)))
    }

    @Test
    fun reminderInThePastTodayRollsToTomorrow() {
        // Now = 08:00, reminder 07:30 -> today passed -> tomorrow 07:30.
        val reminder = contact().initialReminder(at(2026, 8, 4, 8, 0, 0))
        assertNotNull(reminder)
        assertEquals(at(2026, 8, 5, 7, 30, 0), reminder!!.fireAtEpochMillis)
    }

    @Test
    fun nextReminderStillFiresAtNextCheckInDate() {
        val checkedIn = contact().copy(nextCheckInDate = "2026-08-06")
        val reminder = checkedIn.nextReminder(at(2026, 8, 4, 12, 0, 0))
        assertNotNull(reminder)
        assertEquals(at(2026, 8, 6, 7, 30, 0), reminder!!.fireAtEpochMillis)
    }

    @Test
    fun checkInTargetUsesScheduledDateEvenWhenReminderIsStillAheadToday() {
        val scheduled = contact(reminderTime = "17:00:00")
            .copy(nextCheckInDate = "2026-08-04")

        assertEquals(
            at(2026, 8, 4, 17, 0, 0),
            scheduled.nextCheckInTargetEpochMillis(at(2026, 8, 4, 12, 0, 0)),
        )
    }

    @Test
    fun countdownUsesCompactHourAndMinuteLabels() {
        assertEquals("5hr", checkInCountdownLabel(5 * 60 * 60 * 1_000L))
        assertEquals("4hr 59m", checkInCountdownLabel(4 * 60 * 60 * 1_000L + 59 * 60 * 1_000L))
        assertEquals("Now", checkInCountdownLabel(0))
    }

    @Test
    fun nextUpcomingTargetChoosesSoonestFutureContact() {
        val now = at(2026, 8, 4, 12, 0, 0)
        val later = contact("17:00:00").copy(nextCheckInDate = "2026-08-04")
        val sooner = contact("13:30:00").copy(nextCheckInDate = "2026-08-04")
        val overdue = contact("08:00:00").copy(nextCheckInDate = "2026-08-04")

        assertEquals(
            at(2026, 8, 4, 13, 30, 0),
            listOf(later, sooner, overdue).nextUpcomingCheckInTargetEpochMillis(now),
        )
    }
}