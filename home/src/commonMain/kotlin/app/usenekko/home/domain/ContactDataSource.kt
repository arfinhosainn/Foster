package app.usenekko.home.domain

import app.usenekko.shared.domain.Result

interface ContactDataSource {
    suspend fun getContacts(): Result<List<Contact>, ContactError>
    suspend fun createContact(
        name: String,
        avatarColor: String?,
        checkInFrequency: String,
        reminderTime: String?,
    ): Result<Contact, ContactError>
    suspend fun getGroups(): Result<List<Group>, ContactError>
    suspend fun getGroupMemberships(): Result<List<GroupMembership>, ContactError>
    suspend fun createGroup(
        name: String,
        color: String?,
    ): Result<Group, ContactError>
    suspend fun updateGroup(
        groupId: String,
        name: String,
    ): Result<Unit, ContactError>
    suspend fun assignContactToGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError>
    suspend fun removeContactFromGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError>
    suspend fun moveContactToGroup(
        contactId: String,
        fromGroupId: String,
        toGroupId: String,
    ): Result<Unit, ContactError>
    suspend fun deleteGroup(groupId: String): Result<Unit, ContactError>
    suspend fun getCheckIns(
        contactId: String?,
        from: String,
        to: String,
    ): Result<List<CheckIn>, ContactError>
    suspend fun logCheckIn(
        contactId: String,
        lastCheckInDate: String,
        nextCheckInDate: String?,
        streakCount: Int,
    ): Result<Contact, ContactError>
    suspend fun getNotes(contactId: String): Result<List<Note>, ContactError>
    suspend fun createNote(
        contactId: String,
        title: String,
        body: String,
    ): Result<Note, ContactError>
    suspend fun deleteNote(noteId: String): Result<Unit, ContactError>
    suspend fun getReminders(contactId: String): Result<List<Reminder>, ContactError>
    suspend fun createReminder(
        contactId: String,
        title: String,
        description: String,
        recurrence: String,
        date: Long?,
    ): Result<Reminder, ContactError>
    suspend fun deleteReminder(reminderId: String): Result<Unit, ContactError>
    suspend fun getBadges(): Result<List<Badge>, ContactError>
    suspend fun getUserBadges(): Result<List<UserBadge>, ContactError>
}

sealed interface ContactError {
    data object NotAuthenticated : ContactError
    data object Network : ContactError
    data class Unknown(val detail: String?) : ContactError
}
