package app.usefoster.home.domain

import app.usefoster.shared.notifications.DayPlan
import app.usefoster.shared.notifications.MaxScheduledDays
import app.usefoster.shared.notifications.NotificationCategories
import app.usefoster.shared.notifications.StandalonePlan
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * One schedulable notification item. Day-granular items (check-ins, overdue
 * check-ins) collapse into the daily digest; custom reminders schedule
 * standalone at their own time (see docs/notification-system-plan.md §3.2).
 */
sealed interface DueItem {
    val id: String
    val headline: String
    val contactId: String?
    val priority: Int
}

data class CheckInDue(
    override val contactId: String,
    override val headline: String,
    override val priority: Int = PRIORITY_CHECK_IN,
) : DueItem {
    override val id: String = "check_in:$contactId"
}

data class OverdueCheckIn(
    override val contactId: String,
    override val headline: String,
    val daysOverdue: Int,
    override val priority: Int = PRIORITY_OVERDUE,
) : DueItem {
    override val id: String = "overdue:$contactId"
}

/**
 * Display/scheduling item for a custom-reminder occurrence. Scheduling never
 * buckets these into the digest (they go through StandalonePlan at their own
 * clock time); [app.usefoster.home.presentation.dayagenda.DayAgendaScreen] uses
 * them to render reminder rows inside a day's agenda list.
 */
data class CustomReminderDue(
    override val id: String,
    override val headline: String,
    override val contactId: String?,
    val fireAtEpochMillis: Long,
    override val priority: Int = PRIORITY_CUSTOM,
) : DueItem

const val PRIORITY_OVERDUE = 300
const val PRIORITY_CUSTOM = 200
const val PRIORITY_CHECK_IN = 100

/**
 * The fixed daily digest hour. Fires once per day and groups every day-granular
 * item due that day. Intentionally not per-contact `reminder_time` — that time
 * is a preference for check-ins, not a semantic deadline (plan §3.2/§7 D1).
 * Seam: wire the profile's `default_reminder_time` here when available.
 */
const val DEFAULT_DIGEST_HOUR = 9

/** Custom reminders without a stored time-of-day fire at this local hour. */
const val CUSTOM_REMINDER_HOUR = 9

/** Parses a 24h "HH:mm" string; null when absent or malformed. */
fun parseTimeOfDay(value: String?): Pair<Int, Int>? {
    if (value.isNullOrBlank()) return null
    val parts = value.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h to m
}

/**
 * Everything needed to schedule notifications at a point in time: digest items
 * bucketed by epoch day, plus standalone custom-reminder plans.
 */
data class DuePlan(
    val dayItems: Map<Long, List<DueItem>>,
    val standalones: List<StandalonePlan>,
)

/**
 * Builds the full scheduling input from current data:
 *
 *  - A contact due today (or brand-new with a reminder time) becomes a
 *    [CheckInDue] in today's bucket.
 *  - A contact whose `next_check_in_date` is in the past becomes an
 *    [OverdueCheckIn] in TODAY's bucket with a synthesized fire time
 *    (today@digestHour) — never its real past due time (plan §3.5).
 *  - A contact due within the horizon becomes a [CheckInDue] on that day.
 *  - Each custom reminder occurrence becomes a standalone plan at its own
 *    clock time — the stored "HH:mm" time-of-day when present, else the
 *    default custom-reminder hour (plan §3.2 D1). Never into the digest.
 */
fun buildDuePlan(
    contacts: List<Contact>,
    customReminders: List<Reminder>,
    today: LocalDate,
    nowEpochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    horizonDays: Int = 30,
    checkedInTodayContactIds: Set<String> = emptySet(),
): DuePlan {
    val digestItems = mutableMapOf<Long, MutableList<DueItem>>()
    val todayKey = today.toEpochDays()
    val horizonKey = todayKey + horizonDays

    fun addDigest(dayKey: Long, item: DueItem) {
        if (dayKey < todayKey || dayKey > horizonKey) return
        digestItems.getOrPut(dayKey) { mutableListOf() }.add(item)
    }

    contacts.forEach { contact ->
        if (contact.id in checkedInTodayContactIds) return@forEach

        val nextDate = contact.nextCheckInDateLocal()
        val daysOverdue = nextDate?.let { todayKey - it.toEpochDays() }

        when {
            daysOverdue == null -> {
                // Brand-new contact (next_check_in_date still null): participate
                // in today's digest when a reminder time exists at all.
                if (contact.reminderTime != null) {
                    addDigest(todayKey, CheckInDue(contact.id, contact.name))
                }
            }

            daysOverdue > 0L -> {
                val overdue = daysOverdue.toInt()
                addDigest(
                    todayKey,
                    OverdueCheckIn(
                        contactId = contact.id,
                        headline = contact.name,
                        daysOverdue = overdue,
                        priority = PRIORITY_OVERDUE + (overdue * 10).coerceAtMost(100),
                    ),
                )
            }

            else -> addDigest(nextDate.toEpochDays(), CheckInDue(contact.id, contact.name))
        }
    }

    val standalones = customReminders.mapNotNull { reminder ->
        val fireAt = reminder.nextOccurrence(nowEpochMillis, timeZone) ?: return@mapNotNull null
        val fireDay = Instant.fromEpochMilliseconds(fireAt).toLocalDateTime(timeZone).date
        if (fireDay.toEpochDays() > horizonKey) return@mapNotNull null
        StandalonePlan(
            key = "${reminder.id}:${fireDay.toEpochDays()}",
            fireAtEpochMillis = fireAt,
            title = reminder.title,
            category = NotificationCategories.CUSTOM_REMINDERS,
            targetId = reminder.contactId,
            targetIsContact = true,
        )
    }

    // Combined iOS 64-notification budget (plan §3.4): each digest day is one
    // pending notification; standalone custom reminders spend whatever budget
    // remains, soonest first. Dropped occurrences re-enter on later reconciles
    // as earlier ones fire or get delivered — nothing is silently lost forever.
    val digestBudget = digestItems.size.coerceAtMost(MaxScheduledDays)
    val cappedStandalones = standalones
        .sortedBy { it.fireAtEpochMillis }
        .take(MaxScheduledDays - digestBudget)

    return DuePlan(dayItems = digestItems, standalones = cappedStandalones)
}

/**
 * Collapses each day's digest items into one [DayPlan]: fire time is the fixed
 * digest hour, the highest-priority item headlines the copy and picks the
 * category, and single-item days carry their deep-link target.
 */
fun buildDayPlans(
    dayItems: Map<Long, List<DueItem>>,
    digestHour: Int = DEFAULT_DIGEST_HOUR,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    maxDays: Int = MaxScheduledDays,
): List<DayPlan> {
    val digestFireHour = digestHour.coerceIn(0, 23)
    return dayItems.entries
        .mapNotNull { (dayKey, items) ->
            if (items.isEmpty()) return@mapNotNull null
            val day = LocalDate.fromEpochDays(dayKey)
            val fireAt = runCatching {
                LocalDateTime(day.year, day.month, day.day, digestFireHour, 0, 0)
                    .toInstant(timeZone)
                    .toEpochMilliseconds()
            }.getOrNull() ?: return@mapNotNull null

            val top = items.maxBy { it.priority }
            DayPlan(
                dayKey = dayKey,
                fireAtEpochMillis = fireAt,
                itemCount = items.size,
                headline = top.headline,
                category = when (top) {
                    is OverdueCheckIn -> NotificationCategories.MISSED_CHECK_INS
                    is CustomReminderDue -> NotificationCategories.CUSTOM_REMINDERS
                    is CheckInDue -> NotificationCategories.CHECK_INS
                },
                singleTargetId = if (items.size == 1) top.contactId else null,
                singleIsContact = items.size == 1 && top.contactId != null,
            )
        }
        .sortedBy { it.dayKey }
        .take(maxDays)
}

/**
 * Next fire time for a custom reminder: its stored date (or the next cadence
 * occurrence) at [fireHour] local time. `none` reminders are one-shot on their
 * date; the cadence values roll forward from that anchor date. Returns null
 * when the reminder has no date or the cadence is unknown.
 */
fun Reminder.nextOccurrence(
    nowEpochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    fireHour: Int = CUSTOM_REMINDER_HOUR,
): Long? {
    val baseMillis = dateEpochMillis ?: return null
    val baseDate = Instant.fromEpochMilliseconds(baseMillis)
        .toLocalDateTime(TimeZone.UTC).date
    val now = Instant.fromEpochMilliseconds(nowEpochMillis).toLocalDateTime(timeZone)

    // A stored time-of-day is the semantic deadline (D1): "8pm" fires at 8pm.
    // Date-only reminders fall back to the default custom-reminder hour.
    val (fireH, fireM) = parseTimeOfDay(timeOfDay) ?: Pair(fireHour.coerceIn(0, 23), 0)

    fun at(day: LocalDate): Long? = runCatching {
        LocalDateTime(day.year, day.month, day.day, fireH, fireM, 0)
            .toInstant(timeZone)
            .toEpochMilliseconds()
    }.getOrNull()

    val candidate: LocalDate = when (recurrence.lowercase()) {
        "none" -> baseDate
        "daily" -> {
            var day = if (now.date > baseDate) now.date else baseDate
            if (at(day)?.let { it <= nowEpochMillis } == true) {
                day = day.plus(DatePeriod(days = 1))
            }
            day
        }

        "weekly", "biweekly", "monthly", "semiannually", "annually" -> {
            val step = when (recurrence.lowercase()) {
                "weekly" -> DatePeriod(days = 7)
                "biweekly" -> DatePeriod(days = 14)
                "monthly" -> DatePeriod(months = 1)
                "semiannually" -> DatePeriod(months = 6)
                else -> DatePeriod(months = 12)
            }
            var day = baseDate
            var guard = 0
            while (guard++ < 500) {
                val fire = at(day)
                if (fire != null && fire > nowEpochMillis) break
                day = day.plus(step)
            }
            day
        }

        else -> return null
    }

    return at(candidate)
}
