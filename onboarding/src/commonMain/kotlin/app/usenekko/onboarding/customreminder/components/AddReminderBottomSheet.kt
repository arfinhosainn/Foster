package app.usenekko.onboarding.customreminder.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.customreminder.CustomReminderAction
import app.usenekko.onboarding.customreminder.CustomReminderState
import app.usenekko.onboarding.customreminder.toReminderDate
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_close
import org.jetbrains.compose.resources.vectorResource
import nekko.onboarding.generated.resources.action_cancel
import nekko.onboarding.generated.resources.action_done
import nekko.onboarding.generated.resources.cd_close
import nekko.onboarding.generated.resources.field_description
import nekko.onboarding.generated.resources.reminder_date_label
import nekko.onboarding.generated.resources.reminder_occasions_hint
import nekko.onboarding.generated.resources.reminder_recurrence_cd
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderBottomSheet(
    state: CustomReminderState,
    onAction: (CustomReminderAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var recurrenceMenuExpanded by remember { mutableStateOf(false) }
    var datePickerVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            onAction(
                                CustomReminderAction.DraftDateChanged(
                                    date = selectedDateMillis.toReminderDate(),
                                    dateEpochMillis = selectedDateMillis,
                                )
                            )
                        }
                        datePickerVisible = false
                    }
                ) {
                    Text(text = stringResource(Res.string.action_done),
                        color = NekkoTheme.colors.text.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(text = stringResource(Res.string.action_cancel),
                        color = NekkoTheme.colors.text.primary)
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
                todayDateBorderColor = NekkoTheme.colors.text.primary
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = NekkoTheme.colors.fill.tertiary,
                    selectedDayContainerColor = Color(0xFF16A34A),
                    todayContentColor = Color(0xFF16A34A),
                    todayDateBorderColor = Color(0xFF16A34A),
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(CustomReminderAction.BottomSheetDismissed) },
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
                        .clickable { onAction(CustomReminderAction.BottomSheetDismissed) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = NekkoTheme.colors.gray.secondary,
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.editingReminderId == null) "Create Reminder" else "Edit Reminder",
                    style = NekkoTheme.typography.heading3Bold,
                    color = NekkoTheme.colors.text.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(NekkoTheme.colors.fill.tertiary) // Slightly different background for card
                    .padding(20.dp)
            ) {
                Column {
                    BasicTextField(
                        value = state.draftTitle,
                        onValueChange = { onAction(CustomReminderAction.DraftTitleChanged(it)) },
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            color = NekkoTheme.colors.text.primary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = NekkoTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (state.draftTitle.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.reminder_occasions_hint),
                                    fontSize = 20.sp,
                                    color = NekkoTheme.colors.text.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                dismissKeyboard()
                            },
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NekkoTheme.colors.gray.quaternary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = state.draftDescription,
                        onValueChange = { onAction(CustomReminderAction.DraftDescriptionChanged(it)) },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = NekkoTheme.colors.text.primary,
                            fontFamily = NekkoTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        decorationBox = { innerTextField ->
                            if (state.draftDescription.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.field_description),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NekkoTheme.colors.text.quaternary
                                )
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                dismissKeyboard()
                            },
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recurrence Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.reminder_recurrence_cd),
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary
                )

                Box {
                    NekkoActionButton(
                        text = state.draftRecurrence,
                        trailingIcon = Icons.Default.KeyboardArrowDown,
                        onClick = { recurrenceMenuExpanded = true },
                        containerColor = NekkoTheme.colors.background.b1,
                        contentColor = NekkoTheme.colors.text.primary,
                        iconTint = NekkoTheme.colors.text.primary,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = modifier.height(44.dp).width(86.dp)
                    )

                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false },
                        containerColor = NekkoTheme.colors.background.b1,
                        shadowElevation = 10.dp,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        listOf("None", "Daily", "Weekly", "Monthly", "Yearly").forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = NekkoTheme.colors.text.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    onAction(CustomReminderAction.DraftRecurrenceChanged(option))
                                    recurrenceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.reminder_date_label),
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary
                )

                NekkoActionButton(
                    text = state.draftDate,
                    trailingIcon = Icons.Default.KeyboardArrowDown,
                    onClick = { datePickerVisible = true },
                    containerColor = NekkoTheme.colors.background.b1,
                    contentColor = NekkoTheme.colors.text.primary,
                    iconTint = NekkoTheme.colors.text.primary,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = modifier.height(44.dp).width(140.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            NekkoButton(
                text = if (state.editingReminderId == null) "Done" else "Update",
                onClick = { onAction(CustomReminderAction.SaveReminderClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

