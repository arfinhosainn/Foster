package app.usenekko.home.presentation.components

import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.contactIdsCheckedInOn
import app.usenekko.home.domain.isCheckedInToday
import app.usenekko.home.domain.isOutstanding
import app.usenekko.home.domain.localDate
import kotlinx.datetime.LocalDate

fun buildCheckInTimelineEvents(
    checkIns: List<CheckIn>,
    contacts: List<Contact>,
    today: LocalDate,
): List<TimelineEvent> {
    val contactsById = contacts.associateBy(Contact::id)
    val checkedInEvents = checkIns
        .filter { it.contactId in contactsById }
        .mapNotNull { checkIn ->
            val date = checkIn.localDate() ?: return@mapNotNull null
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

    val checkedInTodayContactIds = checkIns.contactIdsCheckedInOn(today)
    val cachedOnlyCheckedInToday = contacts.filter {
        it.isCheckedInToday(today) && it.id !in checkedInTodayContactIds
    }
    val cachedCheckInEvent = if (cachedOnlyCheckedInToday.isEmpty()) {
        null
    } else {
        TimelineEvent(
            date = today,
            checkedIn = true,
            avatars = cachedOnlyCheckedInToday.mapNotNull { avatarResourceForColor(it.avatarColor) },
            avatarCount = cachedOnlyCheckedInToday.size,
        )
    }
    val cachedOnlyCheckedInTodayIds = cachedOnlyCheckedInToday.mapTo(mutableSetOf()) { it.id }
    val outstandingContacts = contacts.filter {
        it.isOutstanding(today) && it.id !in checkedInTodayContactIds
            && it.id !in cachedOnlyCheckedInTodayIds
    }
    val pendingEvent = if (outstandingContacts.isEmpty()) {
        null
    } else {
        TimelineEvent(
            date = today,
            checkedIn = false,
            avatars = outstandingContacts.mapNotNull { avatarResourceForColor(it.avatarColor) },
            avatarCount = outstandingContacts.size,
        )
    }
    return checkedInEvents + listOfNotNull(cachedCheckInEvent, pendingEvent)
}