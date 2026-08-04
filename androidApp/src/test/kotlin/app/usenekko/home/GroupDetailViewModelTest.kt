package app.usenekko.home

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.presentation.settings.GroupDetailAction
import app.usenekko.home.presentation.settings.GroupDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
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
    fun removeMemberRemovesFromGroupOnly() = runTest {
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

            viewModel.onAction(GroupDetailAction.RemoveMember("c1"))
            advanceUntilIdle()

            assertTrue(dataSource.memberships.none { it.contactId == "c1" && it.groupId == "g1" })
            assertEquals(listOf("Bob"), viewModel.state.value.members.map { it.name })
        } finally {
            Dispatchers.resetMain()
        }
    }
}