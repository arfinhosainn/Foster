package app.usenekko.home.presentation.components

import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.MissedCheckIn
import app.usenekko.home.domain.localDate
import app.usenekko.home.domain.nextCheckInDateLocal
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun buildCheckInTimelineEvents(
    checkIns: List<CheckIn>,
    contacts: List<Contact>,
    today: LocalDate,
    missedCheckIns: List<MissedCheckIn> = emptyList(),
): List<TimelineEvent> {
    val contactsById = contacts.associateBy(Contact::id)
    val checkedInByDate = mutableMapOf<LocalDate, MutableSet<String>>()
    val missedByDate = mutableMapOf<LocalDate, MutableSet<String>>()
    checkIns.forEach { checkIn ->
        if (checkIn.contactId !in contactsById) return@forEach
        checkIn.localDate()?.let { date ->
            checkedInByDate.getOrPut(date) { mutableSetOf() }.add(checkIn.contactId)
        }
    }
    contacts.forEach { contact ->
        contact.lastCheckInDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { date ->
                checkedInByDate.getOrPut(date) { mutableSetOf() }.add(contact.id)
            }
    }
    missedCheckIns.forEach { missedCheckIn ->
        if (missedCheckIn.contactId in contactsById) {
            missedByDate.getOrPut(missedCheckIn.scheduledDate) { mutableSetOf() }
                .add(missedCheckIn.contactId)
        }
    }
    val timelineStart = timelineStartForToday(today)

    return List(TIMELINE_SLOT_COUNT) { index ->
        timelineStart.plus(DatePeriod(days = index))
    }.filter { it <= today }.flatMap { date ->
        val completedContactIds = checkedInByDate[date].orEmpty()
        val missedContactIds = missedByDate[date].orEmpty()
        val scheduledContactIds = contacts
            .filter { it.isScheduledOn(date = date, initialDate = today) }
            .mapTo(mutableSetOf(), Contact::id)
        (scheduledContactIds + completedContactIds + missedContactIds).mapNotNull { contactId ->
            contactsById[contactId]?.let { contact ->
                TimelineEvent(
                    date = date,
                    checkedIn = contactId in completedContactIds,
                    missed = contactId in missedContactIds,
                    avatars = avatarResourceForColor(contact.avatarColor)?.let(::listOf).orEmpty(),
                    avatarCount = 1,
                )
            }
        }
    }
}

fun resolveInitialCountdownStartDate(
    existingStartDate: LocalDate?,
    checkIns: List<CheckIn>,
    contacts: List<Contact>,
    today: LocalDate,
    missedCheckIns: List<MissedCheckIn> = emptyList(),
): LocalDate? {
    if (existingStartDate != null) return existingStartDate

    val historicalStartDate = (checkIns.mapNotNull { it.localDate() } +
        missedCheckIns.map { it.scheduledDate }).minOrNull()
    if (historicalStartDate != null) return historicalStartDate

    return today.takeIf {
        contacts.any { contact ->
            contact.lastCheckInDate == null &&
                (contact.nextCheckInDateLocal()?.let { it == today } ?: true)
        }
    }
}

private fun Contact.isScheduledOn(
    date: LocalDate,
    initialDate: LocalDate,
): Boolean {
    val nextDate = nextCheckInDateLocal()
    if (nextCheckInDate != null && nextDate == null) return false

    val anchorDate = nextDate
        ?: lastCheckInDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return date == initialDate
    if (date < anchorDate) return false

    return when (checkInFrequency) {
        "daily" -> true
        "weekly" -> daysFrom(anchorDate, date) % 7 == 0L
        "biweekly" -> daysFrom(anchorDate, date) % 14 == 0L
        "monthly" -> date.day == anchorDate.day.coerceAtMost(daysInMonth(date))
        else -> date == anchorDate
    }
}

private fun daysFrom(anchorDate: LocalDate, date: LocalDate): Long =
    date.toEpochDays() - anchorDate.toEpochDays()

private fun daysInMonth(date: LocalDate): Int {
    val firstDay = LocalDate(date.year, date.month, 1)
    val firstDayOfNextMonth = firstDay.plus(DatePeriod(months = 1))
    return (firstDayOfNextMonth.toEpochDays() - firstDay.toEpochDays()).toInt()
}