package app.usenekko.home

import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.presentation.settings.GroupSettingsAction
import app.usenekko.home.presentation.settings.GroupSettingsViewModel
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
class GroupSettingsViewModelTest {

    @Test
    fun listsGroupsWithRealMemberCounts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(
                    GroupMembership("c1", "g1"),
                    GroupMembership("c2", "g1"),
                    GroupMembership("c3", "g2"),
                ),
            )
            val viewModel = GroupSettingsViewModel(dataSource)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(2, state.groups.size)
            assertEquals(2, state.memberCount("g1"))
            assertEquals(1, state.memberCount("g2"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun createGroupAddsToExistingList() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(groups = listOf(Group("g1", "Family")))
            val viewModel = GroupSettingsViewModel(dataSource)
            advanceUntilIdle()

            viewModel.onAction(GroupSettingsAction.OpenCreateDialog)
            viewModel.onAction(GroupSettingsAction.DraftNameChanged("Work"))
            viewModel.onAction(GroupSettingsAction.CreateGroup)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(2, state.groups.size)
            assertEquals("Work", state.groups.last().name)
            assertEquals(false, state.isCreateDialogOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteGroupRemovesFromListAndClearsMemberships() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                groups = listOf(Group("g1", "Family"), Group("g2", "Friends")),
                memberships = listOf(GroupMembership("c1", "g1"), GroupMembership("c2", "g1")),
            )
            val viewModel = GroupSettingsViewModel(dataSource)
            advanceUntilIdle()

            viewModel.onAction(GroupSettingsAction.DeleteGroup("g1"))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(1, state.groups.size)
            assertEquals("Friends", state.groups.single().name)
            assertEquals(0, dataSource.memberships.count { it.groupId == "g1" })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun saveChangesRenamesGroup() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(groups = listOf(Group("g1", "Family")))
            val viewModel = GroupSettingsViewModel(dataSource)
            advanceUntilIdle()

            viewModel.onAction(GroupSettingsAction.StartEditing)
            viewModel.onAction(GroupSettingsAction.DraftGroupNameChanged("g1", "Close Friends"))
            viewModel.onAction(GroupSettingsAction.SaveChanges)
            advanceUntilIdle()

            assertEquals("Close Friends", viewModel.state.value.groups.single().name)
            assertEquals(false, viewModel.state.value.isEditing)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun warmHomeRepositoryRendersGroupsWithoutAnotherServerBatch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                groups = listOf(Group("g1", "Family")),
                memberships = listOf(GroupMembership("c1", "g1")),
            )
            val repository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "user-1" },
                scope = this,
            )
            repository.load()
            advanceUntilIdle()
            val initialCalls = dataSource.getGroupsCalls

            val viewModel = GroupSettingsViewModel(dataSource, repository)
            advanceUntilIdle()

            assertEquals(listOf("Family"), viewModel.state.value.groups.map { it.name })
            assertEquals(1, viewModel.state.value.memberCount("g1"))
            assertEquals(initialCalls, dataSource.getGroupsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }
}