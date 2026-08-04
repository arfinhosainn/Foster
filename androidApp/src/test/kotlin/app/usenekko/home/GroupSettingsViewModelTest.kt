package app.usenekko.home

import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
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
}