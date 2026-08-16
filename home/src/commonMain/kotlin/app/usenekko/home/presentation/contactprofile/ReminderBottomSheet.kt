package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.home.domain.Reminder
import app.usenekko.theme.NekkoTheme
import kotlinx.coroutines.launch
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_add
import nekko.home.generated.resources.ic_edit
import nekko.home.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    reminders: List<Reminder>,
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
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
            Text(
                text = "Reminders",
                style = NekkoTheme.typography.heading3Bold,
                fontWeight = FontWeight.SemiBold,
                color = NekkoTheme.colors.text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            if (reminders.isEmpty()) {
                EmptyRemindersContent(onAddReminder = onAddReminder)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    reminders.forEach { reminder ->
                        SwipeableReminderRow(
                            reminder = reminder,
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { onDeleteReminder(reminder.id) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                NekkoActionButton(
                    leadingIcon = vectorResource(Res.drawable.ic_add),
                    onClick = onAddReminder,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            }
        }
    }
}

@Composable
private fun EmptyRemindersContent(
    onAddReminder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NekkoActionButton(
            leadingIcon = vectorResource(Res.drawable.ic_add),
            onClick = onAddReminder,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Add reminder",
            fontSize = 20.sp,
            color = NekkoTheme.colors.text.secondary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap on the plus button",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun SwipeableReminderRow(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 154.dp
    val swipeGap = 8.dp
    val swipeDistancePx = with(density) { (actionWidth + swipeGap).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clipToBounds(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            modifier = Modifier
                .width(actionWidth)
                .height(85.dp),
            horizontalArrangement = Arrangement.spacedBy(swipeGap),
        ) {
            ReminderActionButton(
                text = "Edit",
                icon = Res.drawable.ic_edit,
                tint = NekkoTheme.colors.gray.tertiary,
                backgroundColor = NekkoTheme.colors.fill.quaternary,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onEdit()
                },
                modifier = Modifier.weight(1f),
            )
            ReminderActionButton(
                text = "Remove",
                icon = Res.drawable.ic_trashbin,
                tint = NekkoTheme.colors.red.hover,
                backgroundColor = NekkoTheme.colors.red.active,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onDelete()
                },
                modifier = Modifier.weight(1f),
            )
        }

        ReminderCard(
            reminder = reminder,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .pointerInput(reminder.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo(
                                    (offset.value + dragAmount).coerceIn(
                                        -swipeDistancePx,
                                        0f
                                    )
                                )
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                offset.animateTo(
                                    if (offset.value < -swipeDistancePx / 2f) -swipeDistancePx else 0f,
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                    )
                },
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(85.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.background.b1)
            .background(NekkoTheme.colors.fill.tertiary)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = NekkoTheme.typography.heading4Semibold,
                    color = NekkoTheme.colors.text.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Recurrence",
                        tint = NekkoTheme.colors.text.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Recurrence: ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.tertiary,
                    )
                    Text(
                        text = recurrenceToUiLabel(reminder.recurrence),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NekkoTheme.colors.fill.tertiary)
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
    }
}

@Composable
private fun ReminderActionButton(
    text: String,
    icon: DrawableResource,
    tint: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(81.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}