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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.customreminder.CustomReminderAction
import app.usenekko.onboarding.customreminder.CustomReminderState
import app.usenekko.onboarding.customreminder.toReminderDate
import app.usenekko.theme.NekkoTheme

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
                    Text(text = "Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) {
                    Text(text = "Cancel")
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
                selectedDayContainerColor = NekkoTheme.colors.text.primary,
                todayContentColor = NekkoTheme.colors.text.primary,
                todayDateBorderColor = NekkoTheme.colors.text.primary
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(CustomReminderAction.BottomSheetDismissed) },
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
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
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.text.tertiary.copy(alpha = 0.2f))
                        .clickable { onAction(CustomReminderAction.BottomSheetDismissed) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NekkoTheme.colors.text.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
                                    text = "Birthday, Anniversaries, etc",
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
                                    text = "Description",
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
                    text = "Recurrence",
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary
                )
                
                Box {
                    ReminderPickerPill(
                        text = state.draftRecurrence,
                        onClick = { recurrenceMenuExpanded = true }
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
                    text = "Date",
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.secondary
                )

                ReminderPickerPill(
                    text = state.draftDate,
                    onClick = { datePickerVisible = true }
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

@Composable
private fun ReminderPickerPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NekkoTheme.colors.background.b1)
            .clickable(onClick = onClick)
            .padding(start = if (leadingIcon == null) 14.dp else 12.dp, end = 10.dp)
            .padding(vertical = 9.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = NekkoTheme.colors.text.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
