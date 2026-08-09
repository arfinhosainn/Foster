package app.usenekko.home.data.supabase

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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class ContactDto(
    val id: String,
    val name: String,
    @SerialName("avatar_color") val avatarColor: String? = null,
    @SerialName("check_in_frequency") val checkInFrequency: String = "none",
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("next_check_in_date") val nextCheckInDate: String? = null,
    @SerialName("last_check_in_date") val lastCheckInDate: String? = null,
    @SerialName("streak_count") val streakCount: Int = 0,
) {
    fun toDomain() = Contact(
        id = id,
        name = name,
        avatarColor = avatarColor,
        checkInFrequency = checkInFrequency,
        reminderTime = reminderTime,
        nextCheckInDate = nextCheckInDate,
        lastCheckInDate = lastCheckInDate,
        streakCount = streakCount,
    )
}

@Serializable
private data class GroupDto(
    val id: String,
    val name: String,
    val color: String? = null,
) {
    fun toDomain() = Group(
        id = id,
        name = name,
        color = color,
    )
}

@Serializable
private data class GroupMembershipDto(
    @SerialName("contact_id") val contactId: String,
    @SerialName("group_id") val groupId: String,
)

@Serializable
private data class CheckInDto(
    val id: String,
    @SerialName("contact_id") val contactId: String,
    @SerialName("checked_in_at") val checkedInAt: String,
    val note: String? = null,
) {
    fun toDomain() = CheckIn(
        id = id,
        contactId = contactId,
        checkedInAt = checkedInAt,
        note = note,
    )
}

@Serializable
private data class NoteDto(
    val id: String,
    @SerialName("contact_id") val contactId: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val body: String = "",
    @SerialName("created_at") val createdAt: String,
) {
    fun toDomain() = Note(
        id = id,
        contactId = contactId,
        title = title,
        body = body,
        createdAt = createdAt,
    )
}

@Serializable
private data class ReminderDto(
    val id: String,
    @SerialName("contact_id") val contactId: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val description: String = "",
    val recurrence: String = "none",
    @SerialName("date_epoch_millis") val dateEpochMillis: Long? = null,
) {
    fun toDomain() = Reminder(
        id = id,
        contactId = contactId,
        title = title,
        description = description,
        recurrence = recurrence,
        dateEpochMillis = dateEpochMillis,
    )
}

@Serializable
private data class BadgeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val threshold: Int = 0,
) {
    fun toDomain() = Badge(
        id = id,
        name = name,
        description = description.orEmpty(),
        threshold = threshold,
    )
}

@Serializable
private data class UserBadgeDto(
    @SerialName("badge_id") val badgeId: String,
    @SerialName("unlocked_at") val unlockedAt: String,
) {
    fun toDomain() = UserBadge(badgeId = badgeId, unlockedAt = unlockedAt)
}

class SupabaseContactDataSource(
    private val client: SupabaseClient,
) : ContactDataSource {

    override suspend fun getContacts(): Result<List<Contact>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            val contacts = client.postgrest
                .from("contacts")
                .select {
                    filter { eq("owner_id", session.user?.id ?: "") }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ContactDto>()
                .map { it.toDomain() }

            Result.Success(contacts)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun createContact(
        name: String,
        avatarColor: String?,
        checkInFrequency: String,
        reminderTime: String?,
    ): Result<Contact, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val inserted = client.postgrest
                .from("contacts")
                .insert(
                    mapOf(
                        "owner_id" to userId,
                        "name" to name,
                        "avatar_color" to avatarColor,
                        "check_in_frequency" to checkInFrequency,
                        "reminder_time" to reminderTime,
                    )
                ) { select(Columns.list("id", "name", "avatar_color", "check_in_frequency", "reminder_time", "next_check_in_date", "last_check_in_date", "streak_count")) }
                .decodeSingle<ContactDto>()

            Result.Success(inserted.toDomain())
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getGroups(): Result<List<Group>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            val groups = client.postgrest
                .from("groups")
                .select {
                    filter { eq("owner_id", session.user?.id ?: "") }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<GroupDto>()
                .map { it.toDomain() }

            Result.Success(groups)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getGroupMemberships(): Result<List<GroupMembership>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            val memberships = client.postgrest
                .from("contact_groups")
                .select(Columns.list("contact_id", "group_id"))
                .decodeList<GroupMembershipDto>()
                .map { GroupMembership(contactId = it.contactId, groupId = it.groupId) }

            Result.Success(memberships)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun createGroup(
        name: String,
        color: String?,
    ): Result<Group, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val inserted = client.postgrest
                .from("groups")
                .insert(
                    mapOf(
                        "owner_id" to userId,
                        "name" to name,
                        "color" to color,
                    )
                ) { select(Columns.list("id", "name", "color")) }
                .decodeSingle<GroupDto>()

            Result.Success(inserted.toDomain())
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
    ): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("groups")
                .update({ this["name"] = name }) {
                    filter { eq("id", groupId) }
                    filter { eq("owner_id", userId) }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun assignContactToGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("contact_groups")
                .insert(
                    mapOf(
                        "contact_id" to contactId,
                        "group_id" to groupId,
                    )
                )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun removeContactFromGroup(
        contactId: String,
        groupId: String,
    ): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("contact_groups")
                .delete {
                    filter { eq("contact_id", contactId) }
                    filter { eq("group_id", groupId) }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun moveContactToGroup(
        contactId: String,
        fromGroupId: String,
        toGroupId: String,
    ): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("contact_groups")
                .delete {
                    filter { eq("contact_id", contactId) }
                    filter { eq("group_id", fromGroupId) }
                }
            client.postgrest
                .from("contact_groups")
                .insert(
                    mapOf(
                        "contact_id" to contactId,
                        "group_id" to toGroupId,
                    )
                )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("groups")
                .delete {
                    filter { eq("id", groupId) }
                    filter { eq("owner_id", userId) }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getCheckIns(
        contactId: String?,
        from: String,
        to: String,
    ): Result<List<CheckIn>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            val checkIns = client.postgrest
                .from("check_ins")
                .select(Columns.list("id", "contact_id", "checked_in_at", "note")) {
                    if (contactId != null) {
                        filter { eq("contact_id", contactId) }
                    }
                    filter { gte("checked_in_at", "$from 00:00:00") }
                    filter { lte("checked_in_at", "$to 23:59:59.999999") }
                    order("checked_in_at", Order.DESCENDING)
                }
                .decodeList<CheckInDto>()
                .map { it.toDomain() }

            Result.Success(checkIns)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun logCheckIn(
        contactId: String,
        lastCheckInDate: String,
        nextCheckInDate: String?,
        streakCount: Int,
    ): Result<Contact, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("check_ins")
                .insert(mapOf("contact_id" to contactId))

            val updated = client.postgrest
                .from("contacts")
                .update(
                    {
                        this["last_check_in_date"] = lastCheckInDate
                        this["next_check_in_date"] = nextCheckInDate
                        this["streak_count"] = streakCount
                    }
                ) {
                    filter { eq("id", contactId) }
                    filter { eq("owner_id", userId) }
                    select(Columns.list("id", "name", "avatar_color", "check_in_frequency", "reminder_time", "next_check_in_date", "last_check_in_date", "streak_count"))
                }
                .decodeSingle<ContactDto>()

            Result.Success(updated.toDomain())
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getNotes(contactId: String): Result<List<Note>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val notes = client.postgrest
                .from("notes")
                .select(Columns.list("id", "contact_id", "owner_id", "title", "body", "created_at")) {
                    filter { eq("contact_id", contactId) }
                    filter { eq("owner_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NoteDto>()
                .map { it.toDomain() }

            Result.Success(notes)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun createNote(
        contactId: String,
        title: String,
        body: String,
    ): Result<Note, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val inserted = client.postgrest
                .from("notes")
                .insert(
                    mapOf(
                        "owner_id" to userId,
                        "contact_id" to contactId,
                        "title" to title,
                        "body" to body,
                    )
                ) { select(Columns.list("id", "contact_id", "owner_id", "title", "body", "created_at")) }
                .decodeSingle<NoteDto>()

            Result.Success(inserted.toDomain())
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("notes")
                .delete {
                    filter { eq("id", noteId) }
                    filter { eq("owner_id", userId) }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getReminders(contactId: String): Result<List<Reminder>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val reminders = client.postgrest
                .from("custom_reminders")
                .select(Columns.list("id", "contact_id", "owner_id", "title", "description", "recurrence", "date_epoch_millis")) {
                    filter { eq("contact_id", contactId) }
                    filter { eq("owner_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ReminderDto>()
                .map { it.toDomain() }

            Result.Success(reminders)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun createReminder(
        contactId: String,
        title: String,
        description: String,
        recurrence: String,
        date: Long?,
    ): Result<Reminder, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val body = buildJsonObject {
                put("owner_id", userId)
                put("contact_id", contactId)
                put("title", title)
                put("description", description)
                put("recurrence", recurrence)
                put("date_epoch_millis", date?.let { JsonPrimitive(it) } ?: JsonNull)
            }

            val inserted = client.postgrest
                .from("custom_reminders")
                .insert(body) { select(Columns.list("id", "contact_id", "owner_id", "title", "description", "recurrence", "date_epoch_millis")) }
                .decodeSingle<ReminderDto>()

            Result.Success(inserted.toDomain())
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun deleteReminder(reminderId: String): Result<Unit, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            client.postgrest
                .from("custom_reminders")
                .delete {
                    filter { eq("id", reminderId) }
                    filter { eq("owner_id", userId) }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getBadges(): Result<List<Badge>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)

            val badges = client.postgrest
                .from("badges")
                .select {
                    order("threshold", Order.ASCENDING)
                }
                .decodeList<BadgeDto>()
                .map { it.toDomain() }

            Result.Success(badges)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    override suspend fun getUserBadges(): Result<List<UserBadge>, ContactError> {
        return try {
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(ContactError.NotAuthenticated)
            val userId = session.user?.id ?: return Result.Error(ContactError.NotAuthenticated)

            val userBadges = client.postgrest
                .from("user_badges")
                .select(Columns.list("badge_id", "unlocked_at")) {
                    filter { eq("owner_id", userId) }
                }
                .decodeList<UserBadgeDto>()
                .map { it.toDomain() }

            Result.Success(userBadges)
        } catch (e: Exception) {
            Result.Error(mapError(e))
        }
    }

    private fun mapError(e: Exception): ContactError = when {
        e.message?.contains("JWT", ignoreCase = true) == true -> ContactError.NotAuthenticated
        e.message?.contains("network", ignoreCase = true) == true -> ContactError.Network
        e.message?.contains("timeout", ignoreCase = true) == true -> ContactError.Network
        else -> ContactError.Unknown(e.message)
    }
}
