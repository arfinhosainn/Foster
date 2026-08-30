package app.usefoster.home

import app.usefoster.home.domain.Badge
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.UserBadge
import app.usefoster.home.presentation.HomeViewModel
import app.usefoster.home.presentation.badges.BadgeRevealStore
import app.usefoster.shared.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelBadgeRevealTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun contact(id: String) = Contact(
        id = id,
        name = "C$id",
        avatarColor = "#007AFF",
        checkInFrequency = "daily",
        reminderTime = "12:00:00",
        nextCheckInDate = today.minus(DatePeriod(days = 1)).toString(),
        lastCheckInDate = null,
        streakCount = 0,
    )

    @Test
    fun checkInTriggersRevealWhenBadgeThresholdCrossed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
                badges = listOf(Badge("b1", "Seedling", "first", 1)),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()
            try {
                viewModel.checkIn("c1")
                advanceUntilIdle()

                assertEquals("b1", BadgeRevealStore.pending.value?.id)
            } finally {
                BadgeRevealStore.consume()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun checkInDoesNotReTriggerRevealForAlreadyHeldBadge() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
                badges = listOf(Badge("b1", "Seedling", "first", 1)),
                userBadges = listOf(UserBadge("b1", "2026-08-01T10:00:00Z")),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()
            try {
                viewModel.checkIn("c1")
                advanceUntilIdle()

                assertNull(BadgeRevealStore.pending.value)
            } finally {
                BadgeRevealStore.consume()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
