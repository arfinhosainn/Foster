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
}

sealed interface ContactError {
    data object NotAuthenticated : ContactError
    data object Network : ContactError
    data class Unknown(val detail: String?) : ContactError
}
