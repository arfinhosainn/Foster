package app.usenekko.home

import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred

class FakeContactDataSource(
    contacts: List<Contact> = emptyList(),
    groups: List<Group> = emptyList(),
    memberships: List<GroupMembership> = emptyList(),
    checkIns: List<CheckIn> = emptyList(),
) : ContactDataSource {

    var contacts: List<Contact> = contacts
        private set
    var groups: List<Group> = groups
        private set
    var memberships: List<GroupMembership> = memberships
        private set
    var checkIns: List<CheckIn> = checkIns
        private set

    /** When set, [logCheckIn] suspends until the gate completes. */
    var checkInGate: CompletableDeferred<Unit>? = null

    data class LogCheckInCall(
        val contactId: String,
        val lastCheckInDate: String,
        val nextCheckInDate: String?,
        val streakCount: Int,
    )

    val logCheckInCalls = mutableListOf<LogCheckInCall>()

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
    ): Result<Unit, ContactError> = Result.Success(Unit)

    override suspend fun getCheckIns(
        contactId: String?,
        from: String,
        to: String,
    ): Result<List<CheckIn>, ContactError> {
        val filtered = if (contactId == null) checkIns else checkIns.filter { it.contactId == contactId }
        return Result.Success(filtered)
    }

    override suspend fun logCheckIn(
        contactId: String,
        lastCheckInDate: String,
        nextCheckInDate: String?,
        streakCount: Int,
    ): Result<Contact, ContactError> {
        logCheckInCalls += LogCheckInCall(contactId, lastCheckInDate, nextCheckInDate, streakCount)
        checkInGate?.await()
        val index = contacts.indexOfFirst { it.id == contactId }
        if (index == -1) return Result.Error(ContactError.Unknown("contact not found"))
        val updated = contacts[index].copy(
            lastCheckInDate = lastCheckInDate,
            nextCheckInDate = nextCheckInDate,
            streakCount = streakCount,
        )
        contacts = contacts.toMutableList().also { it[index] = updated }
        checkIns = checkIns + CheckIn(
            id = "ci${checkIns.size + 1}",
            contactId = contactId,
            checkedInAt = "${lastCheckInDate}T12:00:00Z",
        )
        return Result.Success(updated)
    }
}
