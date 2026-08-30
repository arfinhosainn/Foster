package app.usefoster.home

import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.computeCheckInUpdate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeCheckInUpdateTest {

    private val today = LocalDate(2026, 8, 2)

    private fun contact(
        frequency: String = "daily",
        last: LocalDate? = null,
        streak: Int = 0,
    ) = Contact(
        id = "c1",
        name = "Arfin",
        avatarColor = "#007AFF",
        checkInFrequency = frequency,
        reminderTime = "12:00:00",
        nextCheckInDate = null,
        lastCheckInDate = last?.toString(),
        streakCount = streak,
    )

    @Test
    fun firstEverCheckInStartsStreak() {
        val update = computeCheckInUpdate(contact(), today)
        assertEquals(today.toString(), update.lastCheckInDate)
        assertEquals(today.plus(DatePeriod(days = 1)).toString(), update.nextCheckInDate)
        assertEquals(1, update.streakCount)
    }

    @Test
    fun dailyConsecutiveIncrementsStreak() {
        val update = computeCheckInUpdate(
            contact(last = today.minus(DatePeriod(days = 1)), streak = 2),
            today,
        )
        assertEquals(3, update.streakCount)
        assertEquals(today.plus(DatePeriod(days = 1)).toString(), update.nextCheckInDate)
    }

    @Test
    fun gapLongerThanWindowResetsStreak() {
        val update = computeCheckInUpdate(
            contact(last = today.minus(DatePeriod(days = 10)), streak = 4),
            today,
        )
        assertEquals(1, update.streakCount)
    }

    @Test
    fun alreadyCheckedInTodayKeepsStreak() {
        val update = computeCheckInUpdate(
            contact(last = today, streak = 5),
            today,
        )
        assertEquals(5, update.streakCount)
    }

    @Test
    fun weeklyMovesNextCheckInBySevenDays() {
        val update = computeCheckInUpdate(
            contact(frequency = "weekly", last = today.minus(DatePeriod(days = 7)), streak = 1),
            today,
        )
        assertEquals(2, update.streakCount)
        assertEquals(today.plus(DatePeriod(days = 7)).toString(), update.nextCheckInDate)
    }

    @Test
    fun monthlyMovesNextCheckInByOneMonth() {
        val update = computeCheckInUpdate(
            contact(frequency = "monthly", last = null),
            today,
        )
        assertEquals(1, update.streakCount)
        assertEquals(LocalDate(2026, 9, 2).toString(), update.nextCheckInDate)
    }

    @Test
    fun noFrequencyLeavesNextDateNull() {
        val update = computeCheckInUpdate(
            contact(frequency = "none", last = null),
            today,
        )
        assertEquals(1, update.streakCount)
        assertNull(update.nextCheckInDate)
    }

    @Test
    fun biweeklyWindowAllowsFourteenDayGap() {
        val update = computeCheckInUpdate(
            contact(frequency = "biweekly", last = today.minus(DatePeriod(days = 14)), streak = 1),
            today,
        )
        assertEquals(2, update.streakCount)
    }
}
