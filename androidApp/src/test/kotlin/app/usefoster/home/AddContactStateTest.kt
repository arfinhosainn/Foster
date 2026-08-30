package app.usefoster.home

import app.usefoster.home.addcontact.AddContactState
import app.usefoster.home.addcontact.withImportedContact
import app.usefoster.home.addcontact.withTimeDialValue
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.GroupMembership
import app.usefoster.home.presentation.components.avatarIndexForColor
import app.usefoster.home.presentation.components.contactsForGroup
import app.usefoster.home.presentation.components.groupMemberCount
import app.usefoster.shared.contacts.ImportedContact
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
    fun editSaveIsDisabledUntilAContactValueChanges() {
        val state = AddContactState(
            editingContactId = "c1",
            name = "Alex",
            initialName = "Alex",
            selectedAvatarIndex = 2,
            initialAvatarIndex = 2,
            selectedGroupId = "g1",
            initialGroupId = "g1",
            initialGroupResolved = true,
            selectedFrequency = "weekly",
            initialFrequency = "weekly",
            selectedHour = 10,
            initialHour = 10,
            selectedMinute = 30,
            initialMinute = 30,
            isAm = true,
            initialIsAm = true,
        )

        assertTrue(!state.hasChanges)
        assertTrue(!state.canSaveChanges)
        assertTrue(state.copy(name = "Alex Cooper").canSaveChanges)
        assertTrue(!state.copy(name = "Alex Cooper", initialName = "Alex Cooper").hasChanges)
    }

    @Test
    fun editSaveWaitsUntilTheOriginalGroupHasLoaded() {
        val state = AddContactState(
            editingContactId = "c1",
            name = "Alex Cooper",
            initialName = "Alex",
            initialGroupResolved = false,
        )

        assertTrue(!state.hasChanges)
        assertTrue(!state.canSaveChanges)
    }

    @Test
    fun importingAContactWithoutPhotoSetsItsNameAndKeepsManualAvatar() {
        val state = AddContactState(
            name = "Old name",
            selectedAvatarIndex = 2,
            importedPhoto = null,
        ).withImportedContact(ImportedContact(name = "Alex Bell"))

        assertEquals("Alex Bell", state.name)
        assertEquals(2, state.selectedAvatarIndex)
        assertNull(state.importedPhoto)
    }

    @Test
    fun importingAnUnnamedContactDoesNotOverwriteTheForm() {
        val state = AddContactState(
            name = "Existing name",
            selectedAvatarIndex = 1,
        ).withImportedContact(ImportedContact(name = "  "))

        assertEquals("Existing name", state.name)
        assertEquals(1, state.selectedAvatarIndex)
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