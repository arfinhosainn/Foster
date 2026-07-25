package app.usenekko.onboarding.customreminder.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.customreminder.CustomReminderState
import app.usenekko.onboarding.customreminder.CustomReminderAction
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
    
    ModalBottomSheet(
        onDismissRequest = { onAction(CustomReminderAction.BottomSheetDismissed) },
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b0,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
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
                    text = "Create Reminder",
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(NekkoTheme.colors.background.b1) // Slightly different background for card
                    .padding(20.dp)
            ) {
                Column {
                    BasicTextField(
                        value = state.draftTitle,
                        onValueChange = { onAction(CustomReminderAction.DraftTitleChanged(it)) },
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = NekkoTheme.colors.text.primary,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (state.draftTitle.isEmpty()) {
                                Text(
                                    text = "Birthday, Anniversaries, etc",
                                    fontSize = 18.sp,
                                    color = NekkoTheme.colors.text.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            innerTextField()
                        }
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
                            color = NekkoTheme.colors.text.primary
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        decorationBox = { innerTextField ->
                            if (state.draftDescription.isEmpty()) {
                                Text(
                                    text = "Description",
                                    fontSize = 16.sp,
                                    color = NekkoTheme.colors.text.tertiary
                                )
                            }
                            innerTextField()
                        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { recurrenceMenuExpanded = true }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.draftRecurrence,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NekkoTheme.colors.text.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NekkoTheme.colors.text.primary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = recurrenceMenuExpanded,
                        onDismissRequest = { recurrenceMenuExpanded = false },
                        modifier = Modifier.background(NekkoTheme.colors.background.b0)
                    ) {
                        listOf("None", "Daily", "Weekly", "Monthly", "Yearly").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option, color = NekkoTheme.colors.text.primary) },
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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NekkoTheme.colors.fill.secondary)
                        .clickable { onAction(CustomReminderAction.DraftDateChanged("Jan 17, 2025")) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = state.draftDate,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NekkoTheme.colors.text.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            NekkoButton(
                text = "Done",
                onClick = { onAction(CustomReminderAction.SaveReminderClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
