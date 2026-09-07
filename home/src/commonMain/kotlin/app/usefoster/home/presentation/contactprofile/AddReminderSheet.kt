package app.usefoster.home.presentation.contactprofile

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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TimeInput
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import app.usefoster.designsystem.buttons.FosterActionButton
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.home.domain.CUSTOM_REMINDER_HOUR
import app.usefoster.home.domain.parseTimeOfDay
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_close
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.action_cancel
import foster.home.generated.resources.action_done
import foster.home.generated.resources.action_save
import foster.home.generated.resources.cd_close
import foster.home.generated.resources.recurrence_none
import foster.home.generated.resources.reminder_create
import foster.home.generated.resources.reminder_date_label
import foster.home.generated.resources.reminder_description_label
import foster.home.generated.resources.reminder_occasions_hint
import foster.home.generated.resources.reminder_recurrence_cd
import foster.home.generated.resources.reminder_time_label
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = draftDateEpochMillis,
        initialDisplayMode = DisplayMode.Input,
    )
    val recurrenceLabel = reminderRecurrenceOptions
        .firstOrNull { (value, _) -> value.equals(draftRecurrence, ignoreCase = true) }
        ?.second
        ?: Res.string.recurrence_none

    if (timePickerVisible) {
        val initial = parseTimeOfDay(draftTimeOfDay)
        val initialHour24 = initial?.first ?: CUSTOM_REMINDER_HOUR
        val initialMinute = initial?.second ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour24,
            initialMinute = initialMinute,
            is24Hour = false,
        )

        AlertDialog(
            onDismissRequest = { timePickerVisible = false },
            containerColor = FosterTheme.colors.background.b1,
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    onTimeChanged("$hour:$minute")
                    timePickerVisible = false
                }) {
                    Text(text = stringResource(Res.string.action_done), color = FosterTheme.colors.text.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerVisible = false }) {
                    Text(text = stringResource(Res.string.action_cancel), color = FosterTheme.colors.text.primary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TimeInput(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                timeSelectorSelectedContainerColor = FosterTheme.colors.fill.tertiary,
                            ),
                        )
                    }
                }
            },
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
                        color = FosterTheme.colors.text.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        color = FosterTheme.colors.text.primary,
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = FosterTheme.colors.background.b1,
                titleContentColor = FosterTheme.colors.text.primary,
                headlineContentColor = FosterTheme.colors.text.primary,
                weekdayContentColor = FosterTheme.colors.text.secondary,
                subheadContentColor = FosterTheme.colors.text.secondary,
                navigationContentColor = FosterTheme.colors.text.primary,
                yearContentColor = FosterTheme.colors.text.primary,
                currentYearContentColor = FosterTheme.colors.text.primary,
                selectedYearContentColor = FosterTheme.colors.background.b1,
                selectedYearContainerColor = FosterTheme.colors.text.primary,
                dayContentColor = FosterTheme.colors.text.primary,
                selectedDayContentColor = FosterTheme.colors.background.b1,
                selectedDayContainerColor = FosterTheme.colors.text.quaternary,
                todayContentColor = FosterTheme.colors.text.primary,
                todayDateBorderColor = FosterTheme.colors.text.primary,
            ),
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = FosterTheme.colors.fill.tertiary,
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
        containerColor = FosterTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle(
                    color = FosterTheme.colors.gray.quaternary,
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
                        tint = FosterTheme.colors.gray.secondary,
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
                    style = FosterTheme.typography.heading3Bold,
                    color = FosterTheme.colors.text.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(FosterTheme.colors.fill.tertiary)
                    .padding(20.dp),
            ) {
                Column {
                    BasicTextField(
                        value = draftTitle,
                        onValueChange = onTitleChanged,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = FosterTheme.colors.text.primary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FosterTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (draftTitle.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.reminder_occasions_hint),
                                    fontSize = 20.sp,
                                    color = FosterTheme.colors.text.tertiary,
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
                            .background(FosterTheme.colors.gray.quaternary),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = draftDescription,
                        onValueChange = onDescriptionChanged,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = FosterTheme.colors.text.primary,
                            fontFamily = FosterTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        decorationBox = { innerTextField ->
                            if (draftDescription.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.reminder_description_label),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FosterTheme.colors.text.quaternary,
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
                    color = FosterTheme.colors.text.secondary,
                )

                Box {
                    FosterActionButton(
                        text = stringResource(recurrenceLabel),
                        trailingIcon = Icons.Default.KeyboardArrowDown,
                        onClick = { recurrenceMenuExpanded = true },
                        containerColor = FosterTheme.colors.background.b1,
                        contentColor = FosterTheme.colors.text.primary,
                        iconTint = FosterTheme.colors.text.primary,
                        iconSize = 18.dp,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.height(44.dp).width(86.dp),
                    )

                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false },
                        containerColor = FosterTheme.colors.background.b1,
                        shadowElevation = 10.dp,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        reminderRecurrenceOptions.forEach { (value, labelRes) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(labelRes),
                                        color = FosterTheme.colors.text.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = FosterTheme.colors.text.tertiary,
                                        modifier = Modifier.size(18.dp),
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
                    color = FosterTheme.colors.text.secondary,
                )

                FosterActionButton(
                    text = formatReminderDate(draftDateEpochMillis),
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { datePickerVisible = true },
                    containerColor = FosterTheme.colors.background.b1,
                    contentColor = FosterTheme.colors.text.primary,
                    iconTint = FosterTheme.colors.text.primary,
                    iconSize = 18.dp,
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
                    color = FosterTheme.colors.text.secondary,
                )

                FosterActionButton(
                    text = formatReminderTime(draftTimeOfDay),
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { timePickerVisible = true },
                    containerColor = FosterTheme.colors.background.b1,
                    contentColor = FosterTheme.colors.text.primary,
                    iconTint = FosterTheme.colors.text.primary,
                    iconSize = 18.dp,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.height(44.dp).width(140.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FosterButton(
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

/** Shows the effective fire time in 12-hour form with AM/PM (e.g. "9:00 AM"). */
private fun formatReminderTime(timeOfDay: String?): String {
    val (h24, m) = parseTimeOfDay(timeOfDay) ?: (CUSTOM_REMINDER_HOUR to 0)
    val displayHour = when (val twelveHour = h24 % 12) {
        0 -> 12
        else -> twelveHour
    }
    val apm = if (h24 < 12) "AM" else "PM"
    return "$displayHour:${m.toString().padStart(2, '0')} $apm"
}
