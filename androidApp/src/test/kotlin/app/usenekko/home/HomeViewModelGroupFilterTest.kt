package app.usenekko.home

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelGroupFilterTest {

    private val cOutstanding = Contact(
        id = "c1",
        name = "Arfin",
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = "12:00:00",
        nextCheckInDate = "2020-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )
    private val cUpToDate = Contact(
        id = "c2",
        name = "Sam",
        avatarColor = "#FF9500",
        checkInFrequency = "weekly",
        reminderTime = "12:00:00",
        nextCheckInDate = "2099-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )
    private val cUngrouped = Contact(
        id = "c3",
        name = "Pat",
        avatarColor = "#34C759",
        checkInFrequency = "monthly",
        reminderTime = "09:00:00",
        nextCheckInDate = "2099-01-01",
        lastCheckInDate = null,
        streakCount = 0,
    )

    @Test
    fun countsChangePerSelectedGroup() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(cOutstanding, cUpToDate, cUngrouped),
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                ),
            )
            val viewModel = HomeViewModel(dataSource)
            advanceUntilIdle()

            // Everyone (no filter): all 3 contacts.
            assertEquals(listOf("g1", "g2"), viewModel.state.value.groups.map { it.id })
            assertEquals(null, viewModel.state.value.selectedGroupId)
            assertEquals(3, viewModel.state.value.totalContactCount)
            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(2, viewModel.state.value.upToDateCount)

            // Family only: c1 (outstanding) + c2 (up-to-date).
            viewModel.onGroupSelected("g1")
            assertEquals("g1", viewModel.state.value.selectedGroupId)
            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(1, viewModel.state.value.upToDateCount)

            // Friends: no members.
            viewModel.onGroupSelected("g2")
            assertEquals(0, viewModel.state.value.outstandingCount)
            assertEquals(0, viewModel.state.value.upToDateCount)

            // Back to Everyone.
            viewModel.onGroupSelected(null)
            assertEquals(null, viewModel.state.value.selectedGroupId)
            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(2, viewModel.state.value.upToDateCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun zeroGroupsStillLoadsEveryone() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(cOutstanding, cUpToDate),
                groups = emptyList(),
                memberships = emptyList(),
            )
            val viewModel = HomeViewModel(dataSource)
            advanceUntilIdle()

            assertEquals(emptyList<Group>(), viewModel.state.value.groups)
            assertEquals(null, viewModel.state.value.selectedGroupId)
            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(1, viewModel.state.value.upToDateCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeContactDataSource(
        private val contacts: List<Contact>,
        private val groups: List<Group>,
        private val memberships: List<GroupMembership>,
    ) : ContactDataSource {
        override suspend fun getContacts(): Result<List<Contact>, ContactError> = Result.Success(contacts)

        override suspend fun createContact(
            name: String,
            avatarColor: String?,
            checkInFrequency: String,
            reminderTime: String?,
        ): Result<Contact, ContactError> = Result.Error(ContactError.Unknown("not used"))

        override suspend fun getGroups(): Result<List<Group>, ContactError> = Result.Success(groups)

        override suspend fun getGroupMemberships(): Result<List<GroupMembership>, ContactError> =
            Result.Success(memberships)

        override suspend fun createGroup(
            name: String,
            color: String?,
        ): Result<Group, ContactError> = Result.Error(ContactError.Unknown("not used"))

        override suspend fun assignContactToGroup(
            contactId: String,
            groupId: String,
        ): Result<Unit, ContactError> = Result.Error(ContactError.Unknown("not used"))
    }
}
