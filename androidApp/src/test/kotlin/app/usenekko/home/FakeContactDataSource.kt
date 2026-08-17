package app.usenekko.home

import app.usenekko.home.domain.Badge
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.domain.Note
import app.usenekko.home.domain.Reminder
import app.usenekko.home.domain.UserBadge
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred

class FakeContactDataSource(
    contacts: List<Contact> = emptyList(),
    groups: List<Group> = emptyList(),
    memberships: List<GroupMembership> = emptyList(),
    checkIns: List<CheckIn> = emptyList(),
    notes: List<Note> = emptyList(),
    reminders: List<Reminder> = emptyList(),
    badges: List<Badge> = emptyList(),
    userBadges: List<UserBadge> = emptyList(),
) : ContactDataSource {

    var contacts: List<Contact> = contacts
        private set
    var groups: List<Group> = groups
        private set
    var memberships: List<GroupMembership> = memberships
        private set
    var checkIns: List<CheckIn> = checkIns
        private set
    var notes: List<Note> = notes
        private set
    var reminders: List<Reminder> = reminders
        private set
    var badges: List<Badge> = badges
        private set
    var userBadges: List<UserBadge> = userBadges
        private set

    var createContactResult: Result<Contact, ContactError>? = null
    var updateContactResult: Result<Contact, ContactError>? = null

    var getContactsCalls: Int = 0
        private set
    var getGroupsCalls: Int = 0
        private set
    var getGroupMembershipsCalls: Int = 0
        private set
    var getCheckInsCalls: Int = 0
        private set
    var getBadgesCalls: Int = 0
        private set
    var getUserBadgesCalls: Int = 0
        private set

    fun resetCounts() {
        getContactsCalls = 0
        getGroupsCalls = 0
        getGroupMembershipsCalls = 0
        getCheckInsCalls = 0
        getBadgesCalls = 0
        getUserBadgesCalls = 0
    }

    /** When set, [getNotes] returns this error instead of the stored notes. */
    var notesError: ContactError? = null

    /** When set, [createNote] fails without storing anything. */
    var createNoteError: ContactError? = null

    /** When set, [deleteNote] fails without removing anything. */
    var deleteNoteError: ContactError? = null

    /** When set, [createNote] suspends until the gate completes. */
    var createNoteGate: CompletableDeferred<Unit>? = null

    /** When set, [getReminders] returns this error instead of the stored reminders. */
    var remindersError: ContactError? = null

    var badgesError: ContactError? = null
    var userBadgesError: ContactError? = null

    /** When set, [createReminder] fails without storing anything. */
    var createReminderError: ContactError? = null

    /** When set, [deleteReminder] fails without removing anything. */
    var deleteReminderError: ContactError? = null

    /** When set, [logCheckIn] suspends until the gate completes. */
    var checkInGate: CompletableDeferred<Unit>? = null

    /** When set, [logCheckIn] returns this backend failure. */
    var logCheckInError: ContactError? = null

    /** When set, [getContacts] suspends until the gate completes. */
    var getContactsGate: CompletableDeferred<Unit>? = null

    data class LogCheckInCall(
        val contactId: String,
        val lastCheckInDate: String,
        val nextCheckInDate: String?,
        val streakCount: Int,
    )

    data class CreateNoteCall(
        val contactId: String,
        val title: String,
        val body: String,
    )

    data class UpdateContactCall(
        val contactId: String,
        val name: String,
        val avatarColor: String?,
        val checkInFrequency: String,
        val reminderTime: String?,
    )

    data class CreateReminderCall(
        val contactId: String,
        val title: String,
        val description: String,
        val recurrence: String,
        val date: Long?,
    )

    val logCheckInCalls = mutableListOf<LogCheckInCall>()
    val createNoteCalls = mutableListOf<CreateNoteCall>()
    val updateContactCalls = mutableListOf<UpdateContactCall>()
    val deletedNoteIds = mutableListOf<String>()
    val createReminderCalls = mutableListOf<CreateReminderCall>()
    val deletedReminderIds = mutableListOf<String>()
    var createContactCalls: Int = 0
        private set

    override suspend fun getContacts(): Result<List<Contact>, ContactError> {
        getContactsCalls++
        getContactsGate?.await()
        return Result.Success(contacts)
    }

    override suspend fun createContact(
        name: String,
        avatarColor: String?,
        checkInFrequency: String,
        reminderTime: String?,
    ): Result<Contact, ContactError> {
        createContactCalls++
        val result = createContactResult ?: return Result.Error(ContactError.Unknown("not used"))
        if (result is Result.Success) contacts = contacts + result.data
        return result
    }

    override suspend fun deleteContact(contactId: String): Result<Unit, ContactError> {
        contacts = contacts.filterNot { it.id == contactId }
        memberships = memberships.filterNot { it.contactId == contactId }
        checkIns = checkIns.filterNot { it.contactId == contactId }
        notes = notes.filterNot { it.contactId == contactId }
        reminders = reminders.filterNot { it.contactId == contactId }
        return Result.Success(Unit)
    }

    override suspend fun updateContact(
        contactId: String,
        name: String,
        avatarColor: String?,
        checkInFrequency: String,
        reminderTime: String?,
    ): Result<Contact, ContactError> {
        updateContactCalls += UpdateContactCall(
            contactId,
            name,
            avatarColor,
            checkInFrequency,
            reminderTime,
        )
        val result = updateContactResult ?: Result.Error(ContactError.Unknown("not used"))
        if (result is Result.Success) {
            val index = contacts.indexOfFirst { it.id == contactId }
            if (index >= 0) {
                contacts = contacts.toMutableList().also { it[index] = result.data }
            }
        }
        return result
    }

    override suspend fun getGroups(): Result<List<Group>, ContactError> {
        getGroupsCalls++
        return Result.Success(groups)
    }

    override suspend fun getGroupMemberships(): Result<List<GroupMembership>, ContactError> {
        getGroupMembershipsCalls++
        return Result.Success(memberships)
    }

    override suspend fun createGroup(
        name: String,
        color: String?,
    ): Result<Group, ContactError> {
        val group = Group(id = "g${groups.size + 1}", name = name, color = color)
        groups = groups + group
        return Result.Success(group)
    }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
    ): Result<Unit, ContactError> {
        val index = groups.indexOfFirst { it.id == groupId }
        if (index == -1) return Result.Error(ContactError.Unknown("group not found"))
        groups = groups.toMutableList().also { it[index] = it[index].copy(name = name) }
        return Result.Success(Unit)
    }

    override suspend fun assignContactToGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError> {
        memberships = memberships + GroupMembership(contactId, groupId)
        return Result.Success(Unit)
    }

    override suspend fun removeContactFromGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError> {
        memberships = memberships.filterNot {
            it.contactId == contactId && it.groupId == groupId
        }
        return Result.Success(Unit)
    }

    override suspend fun moveContactToGroup(
        contactId: String,
        fromGroupId: String,
        toGroupId: String,
    ): Result<Unit, ContactError> {
        memberships = memberships.filterNot {
            it.contactId == contactId && it.groupId == fromGroupId
        } + GroupMembership(contactId, toGroupId)
        return Result.Success(Unit)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit, ContactError> {
        groups = groups.filterNot { it.id == groupId }
        memberships = memberships.filterNot { it.groupId == groupId }
        return Result.Success(Unit)
    }

    override suspend fun getCheckIns(
        contactId: String?,
        from: String,
        to: String,
    ): Result<List<CheckIn>, ContactError> {
        getCheckInsCalls++
        val filtered = if (contactId == null) checkIns else checkIns.filter { it.contactId == contactId }
        return Result.Success(filtered)
    }

    override suspend fun getBadges(): Result<List<Badge>, ContactError> {
        getBadgesCalls++
        return badgesError?.let { Result.Error(it) } ?: Result.Success(badges)
    }

    override suspend fun getUserBadges(): Result<List<UserBadge>, ContactError> {
        getUserBadgesCalls++
        return userBadgesError?.let { Result.Error(it) } ?: Result.Success(userBadges)
    }

    override suspend fun logCheckIn(
        contactId: String,
        lastCheckInDate: String,
        nextCheckInDate: String?,
        streakCount: Int,
    ): Result<Contact, ContactError> {
        logCheckInCalls += LogCheckInCall(contactId, lastCheckInDate, nextCheckInDate, streakCount)
        checkInGate?.await()
        logCheckInError?.let { return Result.Error(it) }
        val index = contacts.indexOfFirst { it.id == contactId }
        if (index == -1) return Result.Error(ContactError.Unknown("contact not found"))
        val updated = contacts[index].copy(
            lastCheckInDate = lastCheckInDate,
            nextCheckInDate = nextCheckInDate,
            streakCount = streakCount,
        )
        contacts = contacts.toMutableList().also { it[index] = updated }
        val newCheckIn = CheckIn(
            id = "ci${checkIns.size + 1}",
            contactId = contactId,
            checkedInAt = "${lastCheckInDate}T12:00:00Z",
        )
        checkIns = checkIns + newCheckIn
        // Mimic the DB trigger: unlock every badge whose threshold the all-time
        // check-in count now reaches, without re-triggering already-held ones.
        val total = checkIns.size
        val held = userBadges.mapTo(mutableSetOf()) { it.badgeId }
        val newly = badges.filter { it.threshold <= total && it.id !in held }
        if (newly.isNotEmpty()) {
            userBadges = userBadges + newly.map {
                UserBadge(it.id, "${lastCheckInDate}T12:00:00Z")
            }
        }
        return Result.Success(updated)
    }

    override suspend fun getNotes(contactId: String): Result<List<Note>, ContactError> {
        notesError?.let { return Result.Error(it) }
        return Result.Success(notes.filter { it.contactId == contactId })
    }

    override suspend fun createNote(
        contactId: String,
        title: String,
        body: String,
    ): Result<Note, ContactError> {
        createNoteCalls += CreateNoteCall(contactId, title, body)
        createNoteError?.let { return Result.Error(it) }
        createNoteGate?.await()
        val note = Note(
            id = "n${notes.size + 1}",
            contactId = contactId,
            title = title,
            body = body,
            createdAt = "2026-08-03T10:00:00Z",
        )
        notes = notes + note
        return Result.Success(note)
    }

    override suspend fun deleteNote(noteId: String): Result<Unit, ContactError> {
        deletedNoteIds += noteId
        deleteNoteError?.let { return Result.Error(it) }
        notes = notes.filterNot { it.id == noteId }
        return Result.Success(Unit)
    }

    override suspend fun getReminders(contactId: String): Result<List<Reminder>, ContactError> {
        remindersError?.let { return Result.Error(it) }
        return Result.Success(reminders.filter { it.contactId == contactId })
    }

    override suspend fun createReminder(
        contactId: String,
        title: String,
        description: String,
        recurrence: String,
        date: Long?,
    ): Result<Reminder, ContactError> {
        createReminderCalls += CreateReminderCall(contactId, title, description, recurrence, date)
        createReminderError?.let { return Result.Error(it) }
        val reminder = Reminder(
            id = "r${reminders.size + 1}",
            contactId = contactId,
            title = title,
            description = description,
            recurrence = recurrence,
            dateEpochMillis = date,
        )
        reminders = reminders + reminder
        return Result.Success(reminder)
    }

    override suspend fun deleteReminder(reminderId: String): Result<Unit, ContactError> {
        deletedReminderIds += reminderId
        deleteReminderError?.let { return Result.Error(it) }
        reminders = reminders.filterNot { it.id == reminderId }
        return Result.Success(Unit)
    }

}
