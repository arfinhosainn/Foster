package app.usenekko.home

import app.usenekko.home.addcontact.AddContactViewModel
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import app.usenekko.shared.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddContactViewModelTest {

    @Test
    fun warmRepositoryProvidesGroupPickerDataWithoutSeparateReads() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = dataSource()
            val repository = repository(dataSource, this)
            repository.load()
            dataSource.resetCounts()

            val viewModel = viewModel(dataSource, repository)
            advanceUntilIdle()

            assertEquals(listOf("Family", "Friends"), viewModel.state.value.groups.map { it.name })
            assertEquals(listOf("Alice"), viewModel.state.value.contacts.map { it.name })
            assertFalse(viewModel.state.value.groupsLoading)
            assertEquals(0, dataSource.getGroupsCalls)
            assertEquals(0, dataSource.getContactsCalls)
            assertEquals(0, dataSource.getGroupMembershipsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun coldRepositoryShowsLoadingBeforeGroupsAreAvailable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = dataSource()
            val repository = repository(dataSource, this)
            val viewModel = viewModel(dataSource, repository)

            assertTrue(viewModel.state.value.groupsLoading)
            assertTrue(viewModel.state.value.groups.isEmpty())

            advanceUntilIdle()

            assertFalse(viewModel.state.value.groupsLoading)
            assertEquals(listOf("Family", "Friends"), viewModel.state.value.groups.map { it.name })
            assertEquals(1, dataSource.getGroupsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refreshedRepositoryUpdatesThePickerThroughItsDerivedFlow() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = dataSource()
            val repository = repository(dataSource, this)
            repository.load()
            val viewModel = viewModel(dataSource, repository)
            advanceUntilIdle()

            dataSource.createGroup("Work", null)
            repository.invalidate()
            repository.load(forceRefresh = true)
            advanceUntilIdle()

            assertEquals(
                listOf("Family", "Friends", "Work"),
                viewModel.state.value.groups.map { it.name },
            )
            assertFalse(viewModel.state.value.groupsLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savingContactInvalidatesSharedSnapshotForAddContactAndHome() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = dataSource()
            val repository = repository(dataSource, this)
            repository.load()
            val homeViewModel = app.usenekko.home.presentation.HomeViewModel(
                dataSource,
                ReminderScheduler(),
                repository,
            )
            val addContactViewModel = viewModel(dataSource, repository)
            advanceUntilIdle()

            val created = contact("c2", "Bob")
            dataSource.createContactResult = Result.Success(created)
            addContactViewModel.onNameChanged("Bob")
            addContactViewModel.onGroupSelected("g1")
            addContactViewModel.submit()
            advanceUntilIdle()

            homeViewModel.loadContacts(forceRefresh = true)
            advanceUntilIdle()

            assertEquals(listOf("Alice", "Bob"), homeViewModel.state.value.contacts.map { it.name })
            assertEquals(listOf("Alice", "Bob"), addContactViewModel.state.value.contacts.map { it.name })
            assertEquals(
                listOf(GroupMembership("c1", "g1"), GroupMembership("c2", "g1")),
                addContactViewModel.state.value.memberships,
            )
            assertTrue(repository.state.value.snapshot?.contacts?.any { it.id == "c2" } == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(
        dataSource: FakeContactDataSource,
        repository: InMemoryHomeRepository,
    ) = AddContactViewModel(
        contactDataSource = dataSource,
        reminderScheduler = ReminderScheduler(),
        subscriptionRepository = FakeSubscriptionRepository(),
        homeRepository = repository,
    )

    private fun repository(
        dataSource: FakeContactDataSource,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = InMemoryHomeRepository(
        contactDataSource = dataSource,
        accountKeyProvider = { "user-1" },
        scope = scope,
    )

    private fun dataSource() = FakeContactDataSource(
        contacts = listOf(contact("c1", "Alice")),
        groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
        memberships = listOf(GroupMembership("c1", "g1")),
        checkIns = listOf(CheckIn("ci1", "c1", "2026-08-14T12:00:00Z")),
    )

    private fun contact(id: String, name: String) = Contact(
        id = id,
        name = name,
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = null,
        nextCheckInDate = null,
        lastCheckInDate = null,
        streakCount = 0,
    )
}