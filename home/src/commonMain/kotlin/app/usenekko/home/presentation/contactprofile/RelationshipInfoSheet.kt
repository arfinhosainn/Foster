package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.home.domain.Reminder
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_add
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipInfoSheet(
    checkInCount: Int,
    nextCheckInDate: String?,
    reminders: List<Reminder>,
    remindersError: String?,
    onDismiss: () -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Relationship Info",
                    style = NekkoTheme.typography.heading3Bold,
                    color = NekkoTheme.colors.text.primary,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.text.tertiary.copy(alpha = 0.2f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NekkoTheme.colors.text.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CheckInStatsCard(
                checkInCount = checkInCount,
                nextCheckInDate = nextCheckInDate,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Reminders",
                style = NekkoTheme.typography.heading3Bold,
                color = NekkoTheme.colors.text.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (remindersError != null) {
                Text(
                    text = remindersError,
                    fontSize = 14.sp,
                    color = NekkoTheme.colors.red.default,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (reminders.isEmpty()) {
                Text(
                    text = "No reminders yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                reminders.forEach { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onEdit = { onEditReminder(reminder.id) },
                        onDelete = { onDeleteReminder(reminder.id) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NekkoActionButton(
                    text = "Add Reminder",
                    leadingIcon = vectorResource(Res.drawable.ic_add),
                    onClick = onAddReminder,
                )
            }
        }
    }
}

@Composable
private fun CheckInStatsCard(
    checkInCount: Int,
    nextCheckInDate: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.secondary)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatColumn(label = "Next check-in", value = formatCheckInDate(nextCheckInDate))
        StatColumn(label = "Total check-ins", value = checkInCount.toString())
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.tertiary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = NekkoTheme.colors.text.primary,
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.tertiary)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = reminder.title,
                style = NekkoTheme.typography.heading4Semibold,
                color = NekkoTheme.colors.text.primary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NekkoTheme.colors.fill.primary)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = formatReminderDate(reminder.dateEpochMillis),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = recurrenceToUiLabel(reminder.recurrence),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.green.active,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) {
                Text(
                    text = "Edit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NekkoTheme.colors.text.primary,
                )
            }
            TextButton(onClick = onDelete) {
                Text(
                    text = "Remove",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NekkoTheme.colors.red.default,
                )
            }
        }
    }
}
