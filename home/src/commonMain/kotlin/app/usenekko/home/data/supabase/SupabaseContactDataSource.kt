package app.usenekko.home.data.supabase

import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    private fun mapError(e: Exception): ContactError = when {
        e.message?.contains("JWT", ignoreCase = true) == true -> ContactError.NotAuthenticated
        e.message?.contains("network", ignoreCase = true) == true -> ContactError.Network
        e.message?.contains("timeout", ignoreCase = true) == true -> ContactError.Network
        else -> ContactError.Unknown(e.message)
    }
}
