package app.usenekko.home

import app.usenekko.home.addcontact.AddContactState
import app.usenekko.home.addcontact.withTimeDialValue
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.presentation.components.avatarIndexForColor
import app.usenekko.home.presentation.components.contactsForGroup
import app.usenekko.home.presentation.components.groupMemberCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddContactStateTest {

    @Test
    fun stepZeroCanAdvanceBeforeContactDetailsAreEntered() {
        assertTrue(AddContactState().canAdvanceFromStep)
    }

    @Test
    fun saveContactRemainsAvailableBeforeContactDetailsAreEntered() {
        assertTrue(AddContactState().canSubmit)
        assertTrue(
            AddContactState(
                name = "Alex",
                selectedAvatarIndex = 0,
            ).canSubmit,
        )
    }

    @Test
    fun changingDialTimePreservesAmPmSelection() {
        val state = AddContactState(isAm = true).withTimeDialValue(11 * 60 + 45)

        assertTrue(state.isAm)
        assertTrue(state.selectedHour == 11)
        assertTrue(state.selectedMinute == 45)
    }

    @Test
    fun avatarColorsMapToTheMatchingAvatarAsset() {
        assertEquals(0, avatarIndexForColor("#FFCC33"))
        assertEquals(5, avatarIndexForColor("#007AFF"))
        assertNull(avatarIndexForColor("#123456"))
    }

    @Test
    fun groupAvatarsShowOnlyTheFirstTwoMembersInContactOrder() {
        val contacts = listOf(
            testContact("c1"),
            testContact("c2"),
            testContact("c3"),
        )
        val memberships = listOf(
            GroupMembership("c3", "g1"),
            GroupMembership("c1", "g1"),
            GroupMembership("c2", "g1"),
        )

        assertEquals(
            listOf("c1", "c2"),
            contactsForGroup("g1", contacts, memberships).map { it.id },
        )
        assertTrue(contactsForGroup("g2", contacts, memberships).isEmpty())
    }

    @Test
    fun largeGroupsShowFourMembersInContactOrder() {
        val contacts = (1..7).map { testContact("c$it") }
        val memberships = contacts.map { contact -> GroupMembership(contact.id, "g1") }

        assertEquals(
            listOf("c1", "c2", "c3", "c4"),
            contactsForGroup("g1", contacts, memberships).map { it.id },
        )
    }

    @Test
    fun groupMemberCountIncludesAllMemberships() {
        val memberships = listOf(
            GroupMembership("c1", "g1"),
            GroupMembership("c2", "g1"),
            GroupMembership("c3", "g1"),
            GroupMembership("c4", "g2"),
        )

        assertEquals(3, groupMemberCount("g1", memberships))
        assertEquals(0, groupMemberCount("g3", memberships))
    }

    private fun testContact(id: String) = Contact(
        id = id,
        name = id,
        avatarColor = "#007AFF",
        checkInFrequency = "weekly",
        reminderTime = null,
        nextCheckInDate = null,
        lastCheckInDate = null,
        streakCount = 0,
    )
}