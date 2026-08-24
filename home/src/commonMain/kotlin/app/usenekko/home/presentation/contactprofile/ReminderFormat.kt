package app.usenekko.home.presentation.contactprofile

import androidx.compose.runtime.Composable
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nekko.home.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import nekko.home.generated.resources.add_freq_biweekly
import nekko.home.generated.resources.add_freq_daily
import nekko.home.generated.resources.add_freq_monthly
import nekko.home.generated.resources.add_freq_semiannually
import nekko.home.generated.resources.add_freq_weekly
import nekko.home.generated.resources.add_freq_yearly
import nekko.home.generated.resources.date_mdY
import nekko.home.generated.resources.recurrence_none
import nekko.home.generated.resources.reminder_choose_date
import nekko.home.generated.resources.reminder_no_upcoming
import org.jetbrains.compose.resources.stringResource
import nekko.home.generated.resources.month_apr
import nekko.home.generated.resources.month_aug
import nekko.home.generated.resources.month_dec
import nekko.home.generated.resources.month_feb
import nekko.home.generated.resources.month_jan
import nekko.home.generated.resources.month_jul
import nekko.home.generated.resources.month_jun
import nekko.home.generated.resources.month_mar
import nekko.home.generated.resources.month_may
import nekko.home.generated.resources.month_nov
import nekko.home.generated.resources.month_oct
import nekko.home.generated.resources.month_sep

/** Recurrence options as (db value, label resource) — db values are canonical. */
val reminderRecurrenceOptions = listOf(
    "none" to Res.string.recurrence_none,
    "daily" to Res.string.add_freq_daily,
    "weekly" to Res.string.add_freq_weekly,
    "biweekly" to Res.string.add_freq_biweekly,
    "monthly" to Res.string.add_freq_monthly,
    "semiannually" to Res.string.add_freq_semiannually,
    "annually" to Res.string.add_freq_yearly,
)

@Composable
fun formatReminderDate(dateEpochMillis: Long?): String {
    if (dateEpochMillis == null) return stringResource(Res.string.reminder_choose_date)
    val date = Instant.fromEpochMilliseconds(dateEpochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return formatMonthDayYear(date)
}

@Composable
fun formatCheckInDate(date: String?): String {
    if (date == null) return stringResource(Res.string.reminder_no_upcoming)
    return runCatching { LocalDate.parse(date) }
        .getOrNull()
        ?.let { formatMonthDayYear(it) }
        ?: stringResource(Res.string.reminder_no_upcoming)
}

private fun monthAbbrRes(month: Month): StringResource = when (month) {
    Month.JANUARY -> Res.string.month_jan
    Month.FEBRUARY -> Res.string.month_feb
    Month.MARCH -> Res.string.month_mar
    Month.APRIL -> Res.string.month_apr
    Month.MAY -> Res.string.month_may
    Month.JUNE -> Res.string.month_jun
    Month.JULY -> Res.string.month_jul
    Month.AUGUST -> Res.string.month_aug
    Month.SEPTEMBER -> Res.string.month_sep
    Month.OCTOBER -> Res.string.month_oct
    Month.NOVEMBER -> Res.string.month_nov
    Month.DECEMBER -> Res.string.month_dec
}

@Composable
private fun formatMonthDayYear(date: LocalDate): String {
    val monthAbbr = stringResource(monthAbbrRes(date.month))
    return stringResource(Res.string.date_mdY, monthAbbr, date.day, date.year)
}
