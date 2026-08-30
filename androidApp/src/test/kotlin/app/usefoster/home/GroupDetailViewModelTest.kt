package app.usefoster.home

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.Group
import app.usefoster.home.domain.GroupMembership
import app.usefoster.home.data.InMemoryHomeRepository
import app.usefoster.home.presentation.HomeViewModel
import app.usefoster.home.presentation.settings.GroupDetailAction
import app.usefoster.home.presentation.settings.GroupDetailViewModel
import app.usefoster.shared.notifications.ReminderScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    private fun contact(id: String, name: String) = Contact(
        id = id,
        name = name,
        avatarColor = null,
        checkInFrequency = "weekly",
        reminderTime = null,
        nextCheckInDate = null,
        lastCheckInDate = null,
        streakCount = 0,
    )

    @Test
    fun listsOnlyMembersOfTheGroup() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice"), contact("c2", "Bob"), contact("c3", "Cal")),
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                    GroupMembership("c3", "g2"),
                ),
            )
            val viewModel = GroupDetailViewModel("g1", dataSource)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(listOf("Alice", "Bob"), state.members.map { it.name })
            // g2 is a valid move target.
            assertEquals(listOf("Friends"), state.otherGroups.map { it.name })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadsTotalCheckInCountsForMembers() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice"), contact("c2", "Bob")),
                groups = listOf(Group("g1", "Family")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                ),
                checkIns = listOf(
                    CheckIn("ci1", "c1", "2026-01-01T12:00:00Z"),
                    CheckIn("ci2", "c1", "2026-01-02T12:00:00Z"),
                    CheckIn("ci3", "c2", "2026-01-03T12:00:00Z"),
                ),
            )
            val viewModel = GroupDetailViewModel("g1", dataSource)
            advanceUntilIdle()

            assertEquals(mapOf("c1" to 2, "c2" to 1), viewModel.state.value.checkInCounts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun moveMemberMovesBetweenGroups() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice"), contact("c2", "Bob")),
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                ),
            )
            val viewModel = GroupDetailViewModel("g1", dataSource)
            advanceUntilIdle()

            viewModel.onAction(GroupDetailAction.MoveMember("c1", "g2"))
            advanceUntilIdle()

            assertTrue(dataSource.memberships.any { it.contactId == "c1" && it.groupId == "g2" })
            assertTrue(dataSource.memberships.none { it.contactId == "c1" && it.groupId == "g1" })
            assertEquals(listOf("Bob"), viewModel.state.value.members.map { it.name })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun removeMemberDeletesContactFromHome() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice"), contact("c2", "Bob")),
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                ),
                checkIns = listOf(
                    CheckIn("ci1", "c1", "2026-08-15T10:00:00Z"),
                    CheckIn("ci2", "c2", "2026-08-15T11:00:00Z"),
                ),
            )
            val repository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "user-1" },
                scope = this,
            )
            repository.load()
            advanceUntilIdle()

            val viewModel = GroupDetailViewModel("g1", dataSource, repository)
            advanceUntilIdle()

            viewModel.onAction(GroupDetailAction.RemoveMember("c1"))
            advanceUntilIdle()

            assertTrue(dataSource.contacts.none { it.id == "c1" })
            assertTrue(dataSource.memberships.none { it.contactId == "c1" && it.groupId == "g1" })
            assertEquals(listOf("Bob"), viewModel.state.value.members.map { it.name })

            val homeViewModel = HomeViewModel(dataSource, ReminderScheduler(), repository)
            advanceUntilIdle()

            assertEquals(listOf("Bob"), homeViewModel.state.value.contacts.map { it.name })
            assertEquals(1, homeViewModel.state.value.totalContactCount)
            assertTrue(homeViewModel.state.value.checkIns.none { it.contactId == "c1" })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun removeMemberKeepsProgressVisibleUntilRefreshCompletes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice"), contact("c2", "Bob")),
                groups = listOf(Group("g1", "Family")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                ),
            )
            val repository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "user-1" },
                scope = this,
            )
            repository.load()
            advanceUntilIdle()

            val viewModel = GroupDetailViewModel("g1", dataSource, repository)
            advanceUntilIdle()
            dataSource.getContactsGate = CompletableDeferred()

            viewModel.onAction(GroupDetailAction.RemoveMember("c1"))
            runCurrent()

            assertTrue(viewModel.state.value.isMutating)
            assertTrue(viewModel.state.value.isRefreshing)
            assertEquals(listOf("Alice", "Bob"), viewModel.state.value.members.map { it.name })

            dataSource.getContactsGate?.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isMutating)
            assertFalse(viewModel.state.value.isRefreshing)
            assertEquals(listOf("Bob"), viewModel.state.value.members.map { it.name })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun warmHomeRepositoryRendersMembersWithoutAnotherServerBatch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice")),
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(GroupMembership("c1", "g1")),
                checkIns = listOf(CheckIn("ci1", "c1", "2026-01-01T12:00:00Z")),
            )
            val repository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "user-1" },
                scope = this,
            )
            repository.load()
            advanceUntilIdle()
            val initialGroupsCalls = dataSource.getGroupsCalls

            val viewModel = GroupDetailViewModel("g1", dataSource, repository)
            advanceUntilIdle()

            assertEquals(listOf("Alice"), viewModel.state.value.members.map { it.name })
            assertEquals(mapOf("c1" to 1), viewModel.state.value.checkInCounts)
            assertEquals(listOf("Friends"), viewModel.state.value.otherGroups.map { it.name })
            assertEquals(initialGroupsCalls, dataSource.getGroupsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun coldHomeRepositoryKeepsMembersLoadingBeforeTheFirstSnapshot() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1", "Alice")),
                groups = listOf(Group("g1", "Family")),
                memberships = listOf(GroupMembership("c1", "g1")),
            )
            val repository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "user-1" },
                scope = this,
            )

            val viewModel = GroupDetailViewModel("g1", dataSource, repository)

            assertTrue(viewModel.state.value.isLoading)
            assertEquals(emptyList<Contact>(), viewModel.state.value.members)

            advanceUntilIdle()

            assertEquals(false, viewModel.state.value.isLoading)
            assertEquals(listOf("Alice"), viewModel.state.value.members.map { it.name })
        } finally {
            Dispatchers.resetMain()
        }
    }
}