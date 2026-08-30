package app.usefoster.home.presentation.components

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.domain.localDate
import app.usefoster.home.domain.nextCheckInDateLocal
import app.usefoster.home.domain.isOutstanding
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun buildCheckInTimelineEvents(
    checkIns: List<CheckIn>,
    contacts: List<Contact>,
    today: LocalDate,
    missedCheckIns: List<MissedCheckIn> = emptyList(),
    initialCountdownStartDate: LocalDate? = null,
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
    val timelineStart = timelineStartForToday(today, initialCountdownStartDate)

    return List(TIMELINE_SLOT_COUNT) { index ->
        timelineStart.plus(DatePeriod(days = index))
    }.filter { it <= today }.flatMap { date ->
        val completedContactIds = checkedInByDate[date].orEmpty()
        val missedContactIds = missedByDate[date].orEmpty()
        val scheduledContactIds = contacts
            .filter { it.isScheduledOn(date = date, initialDate = today) }
            .mapTo(mutableSetOf(), Contact::id)
        // Today's cell must always surface every contact that is due for a check-in
        // right now, even when the recurrence anchor (isScheduledOn) places that
        // contact strictly in the past (overdue / frequency "none" / post-anchor).
        // Otherwise the current-day slot renders as an empty tile despite contacts
        // waiting. Missed-day cells are unaffected: this only runs for date == today.
        val pendingDueTodayIds = if (date == today) {
            contacts
                .filter { it.isOutstanding(today) && it.id !in completedContactIds }
                .mapTo(mutableSetOf(), Contact::id)
        } else {
            emptySet()
        }
        (scheduledContactIds + completedContactIds + missedContactIds + pendingDueTodayIds)
            .mapNotNull { contactId ->
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

    // Before the first check-in, anchor the timeline's first (bottom-left) cell to the
    // earliest scheduled date among contacts that have not started receiving check-ins yet.
    // This keeps a fresh timeline starting from that first cell across app restarts, instead
    // of collapsing to today's centered rolling window once the first due date is no longer
    // exactly today. The next check-in date is durable server data, so it survives relaunch.
    val firstScheduledDate = contacts
        .filter { it.lastCheckInDate == null }
        .mapNotNull { it.nextCheckInDateLocal() }
        .minOrNull()
    if (firstScheduledDate != null) return firstScheduledDate

    // No explicit schedule yet — a not-yet-started contact defaults to today's first cell.
    return today.takeIf {
        contacts.any { it.lastCheckInDate == null && it.nextCheckInDateLocal() == null }
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