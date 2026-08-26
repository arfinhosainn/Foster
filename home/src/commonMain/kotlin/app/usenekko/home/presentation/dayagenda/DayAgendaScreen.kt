package app.usenekko.home.presentation.dayagenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.home.di.LocalContactDataSource
import app.usenekko.home.domain.CheckInDue
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.CustomReminderDue
import app.usenekko.home.domain.DueItem
import app.usenekko.home.domain.OverdueCheckIn
import app.usenekko.home.domain.buildDuePlan
import app.usenekko.home.domain.isCheckedInToday
import app.usenekko.home.domain.nextCheckInDateLocal
import app.usenekko.home.domain.nextOccurrence
import app.usenekko.shared.domain.Result
import app.usenekko.theme.NekkoTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Grouped day view opened from a digest notification tap: everything due on
 * the tapped day in one list — due/overdue check-ins (with Check-In action)
 * and custom reminders falling on that date.
 */
@Composable
fun DayAgendaScreen(
    dayKey: Long,
    onBack: () -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contactDataSource = LocalContactDataSource.current
    var items by remember { mutableStateOf<List<DueItem>>(emptyList()) }
    var contactsById by remember { mutableStateOf<Map<String, Contact>>(emptyMap()) }

    LaunchedEffect(dayKey) {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(timeZone)
        val day = LocalDate.fromEpochDays(dayKey)
        val now = Clock.System.now().toEpochMilliseconds()

        val contacts = when (val result = contactDataSource.getContacts()) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }
        contactsById = contacts.associateBy { it.id }

        val reminders = when (val result = contactDataSource.getAllCustomReminders()) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }

        val plan = buildDuePlan(
            contacts = contacts,
            customReminders = reminders,
            today = today,
            nowEpochMillis = now,
        )
        val dayItems = plan.dayItems[dayKey].orEmpty().toMutableList()

        reminders.forEach { reminder ->
            val fireAt = reminder.nextOccurrence(now) ?: return@forEach
            val fireDay = Instant.fromEpochMilliseconds(fireAt).toLocalDateTime(timeZone).date
            if (fireDay == day) {
                dayItems += CustomReminderDue(
                    id = "custom:${reminder.id}:$dayKey",
                    headline = reminder.title,
                    contactId = reminder.contactId,
                    fireAtEpochMillis = fireAt,
                )
            }
        }

        // The agenda can be opened for a past day (e.g. after the digest rolled
        // forward): fall back to a static view of who was due that day.
        if (dayItems.isEmpty() && day != today) {
            contacts.forEach { contact ->
                if (contact.nextCheckInDateLocal() == day && !contact.isCheckedInToday(today)) {
                    dayItems += CheckInDue(contact.id, contact.name)
                }
            }
        }
        items = dayItems.sortedByDescending { it.priority }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NekkoTheme.colors.gray.primary,
                )
            }
            Text(
                text = formatDateLabel(LocalDate.fromEpochDays(dayKey)),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = NekkoTheme.colors.text.primary,
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nothing due this day",
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    DayAgendaRow(
                        item = item,
                        contactName = item.contactId?.let { contactsById[it]?.name },
                        onContactClick = onContactClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayAgendaRow(
    item: DueItem,
    contactName: String?,
    onContactClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NekkoTheme.colors.fill.quaternary, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                item.contactId?.let(onContactClick)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (item) {
                    is OverdueCheckIn -> "${item.headline} · ${item.daysOverdue}d overdue"
                    else -> item.headline
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.primary,
            )
            Text(
                text = when (item) {
                    is OverdueCheckIn -> "Missed check-in"
                    is CustomReminderDue -> "Reminder"
                    is CheckInDue -> "Check-in due"
                },
                fontSize = 13.sp,
                color = NekkoTheme.colors.text.tertiary,
            )
        }
        if (item is CheckInDue || item is OverdueCheckIn) {
            Spacer(Modifier.width(8.dp))
            NekkoButton(
                text = "Check-In",
                onClick = { item.contactId?.let(onContactClick) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private fun formatDateLabel(day: LocalDate): String {
    val month = day.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${day.day}, ${day.year}"
}
