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

    @Test
    fun editingContactPrefillsAndPersistsContactAndGroupChanges() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = dataSource()
            val repository = repository(dataSource, this)
            val existing = dataSource.contacts.single()
            val updated = existing.copy(
                name = "Alice Cooper",
                avatarColor = "#FF3B30",
                checkInFrequency = "monthly",
                reminderTime = "18:45:00",
            )
            dataSource.updateContactResult = Result.Success(updated)

            val viewModel = AddContactViewModel(
                contactDataSource = dataSource,
                reminderScheduler = ReminderScheduler(),
                subscriptionRepository = FakeSubscriptionRepository(),
                homeRepository = repository,
                editingContact = existing,
            )
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isEditing)
            assertEquals("Alice", viewModel.state.value.name)
            assertEquals(5, viewModel.state.value.selectedAvatarIndex)
            assertEquals("weekly", viewModel.state.value.selectedFrequency)
            assertEquals("g1", viewModel.state.value.selectedGroupId)

            viewModel.onNameChanged("Alice Cooper")
            viewModel.onAvatarSelected(3)
            viewModel.onFrequencySelected("monthly")
            viewModel.onTimeSelected(6, 45, false)
            viewModel.onGroupSelected("g2")
            viewModel.onNextStep()
            viewModel.onNextStep()
            viewModel.onNextStep()
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FakeContactDataSource.UpdateContactCall(
                        contactId = "c1",
                        name = "Alice Cooper",
                        avatarColor = "#FF3B30",
                        checkInFrequency = "monthly",
                        reminderTime = "18:45:00",
                    ),
                ),
                dataSource.updateContactCalls,
            )
            assertEquals("Alice Cooper", dataSource.contacts.single().name)
            assertEquals(listOf(GroupMembership("c1", "g2")), dataSource.memberships)
            assertFalse(viewModel.state.value.isSubmitting)
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