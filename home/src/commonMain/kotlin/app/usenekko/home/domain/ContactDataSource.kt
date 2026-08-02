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
    suspend fun assignContactToGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError>
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
}

sealed interface ContactError {
    data object NotAuthenticated : ContactError
    data object Network : ContactError
    data class Unknown(val detail: String?) : ContactError
}
