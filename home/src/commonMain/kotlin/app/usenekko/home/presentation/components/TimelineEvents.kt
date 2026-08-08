package app.usenekko.home.presentation.components

import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.isOutstanding
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun buildCheckInTimelineEvents(
    checkIns: List<CheckIn>,
    contacts: List<Contact>,
    today: LocalDate,
): List<TimelineEvent> {
    val contactsById = contacts.associateBy(Contact::id)
    val checkedInEvents = checkIns
        .filter { it.contactId in contactsById }
        .mapNotNull { checkIn ->
            val date = runCatching {
                Instant.parse(checkIn.checkedInAt)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }.getOrNull() ?: return@mapNotNull null
            date to checkIn
        }
        .groupBy({ it.first }, { it.second })
        .map { (date, rows) ->
            TimelineEvent(
                date = date,
                checkedIn = true,
                avatars = rows.mapNotNull { contactsById[it.contactId]?.avatarColor }
                    .mapNotNull(::avatarResourceForColor),
                avatarCount = rows.size,
            )
        }

    val outstandingContacts = contacts.filter { it.isOutstanding(today) }
    return if (outstandingContacts.isEmpty()) {
        checkedInEvents
    } else {
        checkedInEvents + TimelineEvent(
            date = today,
            checkedIn = false,
            avatars = outstandingContacts.mapNotNull { avatarResourceForColor(it.avatarColor) },
            avatarCount = outstandingContacts.size,
        )
    }
}