package app.usenekko.home

import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.domain.Note
import app.usenekko.home.domain.Reminder
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred

class FakeContactDataSource(
    contacts: List<Contact> = emptyList(),
    groups: List<Group> = emptyList(),
    memberships: List<GroupMembership> = emptyList(),
    checkIns: List<CheckIn> = emptyList(),
    notes: List<Note> = emptyList(),
    reminders: List<Reminder> = emptyList(),
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

    /** When set, [getNotes] returns this error instead of the stored notes. */
    var notesError: ContactError? = null

    /** When set, [createNote] fails without storing anything. */
    var createNoteError: ContactError? = null

    /** When set, [createNote] suspends until the gate completes. */
    var createNoteGate: CompletableDeferred<Unit>? = null

    /** When set, [getReminders] returns this error instead of the stored reminders. */
    var remindersError: ContactError? = null

    /** When set, [createReminder] fails without storing anything. */
    var createReminderError: ContactError? = null

    /** When set, [deleteReminder] fails without removing anything. */
    var deleteReminderError: ContactError? = null

    /** When set, [logCheckIn] suspends until the gate completes. */
    var checkInGate: CompletableDeferred<Unit>? = null

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

    data class CreateReminderCall(
        val contactId: String,
        val title: String,
        val description: String,
        val recurrence: String,
        val date: Long?,
    )

    val logCheckInCalls = mutableListOf<LogCheckInCall>()
    val createNoteCalls = mutableListOf<CreateNoteCall>()
    val createReminderCalls = mutableListOf<CreateReminderCall>()
    val deletedReminderIds = mutableListOf<String>()

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
