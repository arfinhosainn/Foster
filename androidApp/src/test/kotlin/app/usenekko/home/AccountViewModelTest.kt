package app.usenekko.home

import app.usenekko.home.domain.Badge
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.UserBadge
import app.usenekko.home.presentation.settings.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private fun contact(id: String, name: String = "x") = Contact(
        id = id,
        name = name,
        avatarColor = null,
        checkInFrequency = "weekly",
        reminderTime = null,
        nextCheckInDate = "2026-08-10",
        lastCheckInDate = null,
        streakCount = 0,
    )

    private fun checkIn(contactId: String) =
        CheckIn(id = "ci_$contactId$contactId", contactId = contactId, checkedInAt = "2026-08-01T10:00:00Z")

    @Test
    fun accountLoadsRealProfileAndCounts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val profileDataSource = FakeProfileDataSource()
            val contactDataSource = FakeContactDataSource(
                contacts = listOf(contact("c1"), contact("c2")),
                checkIns = listOf(
                    checkIn("c1"), checkIn("c1"), checkIn("c2"), checkIn("c2"),
                ),
            )
            val viewModel = AccountViewModel(profileDataSource, contactDataSource)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("Jane Bell", state.fullName)
            assertEquals("2026-01-15T10:00:00Z", state.createdAt)
            assertEquals(2, state.totalContacts)
            // Total check-ins across ALL contacts, derived from rows (no new column).
            assertEquals(4, state.totalCheckIns)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun missingProfileLeavesNameNullWithoutCrashing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val profileDataSource = FakeProfileDataSource().apply { profile = null }
            val contactDataSource = FakeContactDataSource(contacts = emptyList())
            val viewModel = AccountViewModel(profileDataSource, contactDataSource)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(false, state.isLoading)
            assertNull(state.fullName)
            assertEquals(0, state.totalContacts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun accountLoadsBadgeSlotsWithUnlockStateSortedByThreshold() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val profileDataSource = FakeProfileDataSource()
            val contactDataSource = FakeContactDataSource(
                contacts = emptyList(),
                badges = listOf(
                    Badge("b2", "Wild Flower", "bloom", 15),
                    Badge("b1", "Seedling", "first", 1),
                ),
                userBadges = listOf(UserBadge("b1", "2026-08-04T10:00:00Z")),
            )
            val viewModel = AccountViewModel(profileDataSource, contactDataSource)
            advanceUntilIdle()

            val slots = viewModel.state.value.badgeSlots
            assertEquals(2, slots.size)
            assertEquals("b1", slots[0].badge.id)
            assertEquals(true, slots[0].unlocked)
            assertEquals("b2", slots[1].badge.id)
            assertEquals(false, slots[1].unlocked)
        } finally {
            Dispatchers.resetMain()
        }
    }
}