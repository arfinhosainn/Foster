package app.usenekko.home

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.computeReminderPlans
import app.usenekko.home.domain.initialReminder
import app.usenekko.home.domain.nextReminder
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelReminderReconcileTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val now = Clock.System.now().toEpochMilliseconds()

    private fun contact(
        id: String,
        reminderTime: String? = "07:30:00",
        nextCheckInDate: String? = null,
    ) = Contact(
        id = id,
        name = "C$id",
        avatarColor = "#007AFF",
        checkInFrequency = "daily",
        reminderTime = reminderTime,
        nextCheckInDate = nextCheckInDate,
        lastCheckInDate = null,
        streakCount = 0,
    )

    @Test
    fun freshContactStaysInPlansDespiteNullNextCheckInDate() {
        // The regression: Home's reconciliation used to drop a brand-new contact
        // (next_check_in_date == null) from the plan and then cancel its alarm,
        // so the first reminder never fired.
        val fresh = contact(id = "c1")

        val plans = listOf(fresh).computeReminderPlans(now)

        val plan = plans.firstOrNull { it.contactId == "c1" }
        assertNotNull("fresh contact must remain scheduled after reconciliation", plan)
        assertEquals(fresh.initialReminder(now)!!.fireAtEpochMillis, plan!!.fireAtEpochMillis)
    }

    @Test
    fun checkedInContactPlansFromNextCheckInDate() {
        val checkedIn = contact(
            id = "c2",
            nextCheckInDate = today.plus(DatePeriod(days = 1)).toString(),
        )

        val plans = listOf(checkedIn).computeReminderPlans(now)

        val plan = plans.firstOrNull { it.contactId == "c2" }
        assertNotNull(plan)
        assertEquals(checkedIn.nextReminder(now)!!.fireAtEpochMillis, plan!!.fireAtEpochMillis)
    }

    @Test
    fun missingOrMalformedReminderTimeYieldsNoPlan() {
        val noTime = contact(id = "c3", reminderTime = null)
        val badTime = contact(id = "c4", reminderTime = "nope")
        val plans = listOf(noTime, badTime).computeReminderPlans(now)
        assertTrue(plans.isEmpty())
    }
}
