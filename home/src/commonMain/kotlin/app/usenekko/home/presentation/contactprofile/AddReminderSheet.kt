package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.home.domain.CUSTOM_REMINDER_HOUR
import app.usenekko.home.domain.parseTimeOfDay
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_close
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.action_cancel
import nekko.home.generated.resources.action_done
import nekko.home.generated.resources.action_save
import nekko.home.generated.resources.cd_close
import nekko.home.generated.resources.reminder_create
import nekko.home.generated.resources.reminder_date_label
import nekko.home.generated.resources.reminder_description_label
import nekko.home.generated.resources.reminder_occasions_hint
import nekko.home.generated.resources.reminder_recurrence_cd
import nekko.home.generated.resources.reminder_time_label
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    draftTitle: String,
    draftDescription: String,
    draftRecurrence: String,
    draftDateEpochMillis: Long?,
    draftTimeOfDay: String? = null,
    isSaving: Boolean,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onRecurrenceChanged: (String) -> Unit,
    onDateChanged: (Long?) -> Unit,
    onTimeChanged: (String?) -> Unit = {},
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var recurrenceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var datePickerVisible by rememberSaveable { mutableStateOf(false) }
    var timePickerVisible by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = draftDateEpochMillis)

    if (timePickerVisible) {
        val initial = parseTimeOfDay(draftTimeOfDay)
        val timeState = rememberTimePickerState(
            initialHour = initial?.first ?: CUSTOM_REMINDER_HOUR,
            initialMinute = initial?.second ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerVisible = false },
            containerColor = NekkoTheme.colors.background.b1,
            confirmButton = {
                TextButton(onClick = {
                    // CommonMain-safe zero-padded "HH:mm"
                    val h = timeState.hour.toString().padStart(2, '0')
                    val m = timeState.minute.toString().padStart(2, '0')
                    onTimeChanged("$h:$m")
                    timePickerVisible = false
                }) {
                    Text(text = stringResource(Res.string.action_done), color = NekkoTheme.colors.text.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerVisible = false }) {
                    Text(text = stringResource(Res.string.action_cancel), color = NekkoTheme.colors.text.primary)
                }
            },
            text = { TimePicker(state = timeState) },
        )
    }

    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChanged(datePickerState.selectedDateMillis)
                        datePickerVisible = false
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.action_done),
                        color = NekkoTheme.colors.text.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        color = NekkoTheme.colors.text.primary,
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = NekkoTheme.colors.background.b1,
                titleContentColor = NekkoTheme.colors.text.primary,
                headlineContentColor = NekkoTheme.colors.text.primary,
                weekdayContentColor = NekkoTheme.colors.text.secondary,
                subheadContentColor = NekkoTheme.colors.text.secondary,
                navigationContentColor = NekkoTheme.colors.text.primary,
                yearContentColor = NekkoTheme.colors.text.primary,
                currentYearContentColor = NekkoTheme.colors.text.primary,
                selectedYearContentColor = NekkoTheme.colors.background.b1,
                selectedYearContainerColor = NekkoTheme.colors.text.primary,
                dayContentColor = NekkoTheme.colors.text.primary,
                selectedDayContentColor = NekkoTheme.colors.background.b1,
                selectedDayContainerColor = NekkoTheme.colors.text.quaternary,
                todayContentColor = NekkoTheme.colors.text.primary,
                todayDateBorderColor = NekkoTheme.colors.text.primary,
            ),
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = NekkoTheme.colors.fill.tertiary,
                    selectedDayContainerColor = Color(0xFF16A34A),
                    todayContentColor = Color(0xFF16A34A),
                    todayDateBorderColor = Color(0xFF16A34A),
                ),
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle(
                    color = NekkoTheme.colors.gray.quaternary,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp, top = 10.dp)
                        .clip(CircleShape)
                        .background(Color.Unspecified)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = NekkoTheme.colors.gray.secondary,
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .imePadding(),
            ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.reminder_create),
                    style = NekkoTheme.typography.heading3Bold,
                    color = NekkoTheme.colors.text.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(NekkoTheme.colors.fill.tertiary)
                    .padding(20.dp),
            ) {
                Column {
                    BasicTextField(
                        value = draftTitle,
                        onValueChange = onTitleChanged,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = NekkoTheme.colors.text.primary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = NekkoTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (draftTitle.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.reminder_occasions_hint),
                                    fontSize = 20.sp,
                                    color = NekkoTheme.colors.text.tertiary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            innerTextField()
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NekkoTheme.colors.gray.quaternary),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = draftDescription,
                        onValueChange = onDescriptionChanged,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = NekkoTheme.colors.text.primary,
                            fontFamily = NekkoTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        decorationBox = { innerTextField ->
                            if (draftDescription.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.reminder_description_label),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NekkoTheme.colors.text.quaternary,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.reminder_recurrence_cd),
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary,
                )

                Box {
                    NekkoActionButton(
                        text = draftRecurrence,
                        trailingIcon = Icons.Default.KeyboardArrowDown,
                        onClick = { recurrenceMenuExpanded = true },
                        containerColor = NekkoTheme.colors.background.b1,
                        contentColor = NekkoTheme.colors.text.primary,
                        iconTint = NekkoTheme.colors.text.primary,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.height(44.dp).width(86.dp),
                    )

                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false },
                        containerColor = NekkoTheme.colors.background.b1,
                        shadowElevation = 10.dp,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        reminderRecurrenceOptions.forEach { (value, labelRes) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(labelRes),
                                        color = NekkoTheme.colors.text.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                onClick = {
                                    onRecurrenceChanged(value)
                                    recurrenceMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.reminder_date_label),
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary,
                )

                NekkoActionButton(
                    text = formatReminderDate(draftDateEpochMillis),
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { datePickerVisible = true },
                    containerColor = NekkoTheme.colors.background.b1,
                    contentColor = NekkoTheme.colors.text.primary,
                    iconTint = NekkoTheme.colors.text.primary,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.height(44.dp).width(140.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.reminder_time_label),
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary,
                )

                NekkoActionButton(
                    text = formatReminderTime(draftTimeOfDay),
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { timePickerVisible = true },
                    containerColor = NekkoTheme.colors.background.b1,
                    contentColor = NekkoTheme.colors.text.primary,
                    iconTint = NekkoTheme.colors.text.primary,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.height(44.dp).width(140.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            NekkoButton(
                text = if (isEditing) "Update" else stringResource(Res.string.action_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = draftTitle.isNotBlank() && !isSaving,
                loading = isSaving,
            )
            }
        }
    }
}

/** Shows the effective fire time: stored "HH:mm", or the fallback hour when null. */
private fun formatReminderTime(timeOfDay: String?): String {
    val (h, m) = parseTimeOfDay(timeOfDay)
        ?: return CUSTOM_REMINDER_HOUR.toString().padStart(2, '0') + ":00"
    return h.toString().padStart(2, '0') + ":" + m.toString().padStart(2, '0')
}
