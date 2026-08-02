package app.usenekko.home.addcontact

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.home.domain.Group
import app.usenekko.theme.NekkoTheme

private val frequencies = listOf(
    "daily" to "Daily",
    "weekly" to "Weekly",
    "biweekly" to "Bi-weekly",
    "monthly" to "Monthly",
)

private data class StepMeta(
    val title: String,
    val subtitle: String,
)

private val steps = listOf(
    StepMeta("Who do you want to keep in touch with?", "Add a new contact"),
    StepMeta("Add to a group", "Pick a group or create a new one"),
    StepMeta("How often should we remind you?", "Choose your check-in frequency"),
    StepMeta("Set a reminder time", "When should you be reminded?"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddContactEvent.Saved -> onSaved()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NekkoTheme.colors.background.b0,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
        modifier = modifier,
    ) {
        AddContactSheetContent(
            state = state,
            onNameChanged = viewModel::onNameChanged,
            onColorSelected = viewModel::onColorSelected,
            onGroupSelected = viewModel::onGroupSelected,
            onCreateGroupClicked = viewModel::onCreateGroupClicked,
            onFrequencySelected = viewModel::onFrequencySelected,
            onTimeSelected = viewModel::onTimeSelected,
            onBackStep = viewModel::onBackStep,
            onNextStep = viewModel::onNextStep,
            onSubmit = viewModel::submit,
        )
    }

    if (state.showCreateGroupSheet) {
        CreateGroupSheet(
            isSaving = state.isCreatingGroup,
            onDismiss = viewModel::onDismissCreateGroupSheet,
            onSave = viewModel::onSaveGroup,
        )
    }
}

@Composable
private fun AddContactSheetContent(
    state: AddContactState,
    onNameChanged: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onGroupSelected: (String) -> Unit,
    onCreateGroupClicked: () -> Unit,
    onFrequencySelected: (String) -> Unit,
    onTimeSelected: (Int, Int, Boolean) -> Unit,
    onBackStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepMeta = steps[state.currentStep]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        StepProgress(currentStep = state.currentStep)

        Spacer(Modifier.height(24.dp))

        Text(
            text = stepMeta.title,
            style = NekkoTheme.typography.heading1Bold,
            color = NekkoTheme.colors.text.primary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stepMeta.subtitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.tertiary,
        )

        Spacer(Modifier.height(28.dp))

        when (state.currentStep) {
            0 -> NameAndAvatarStep(
                state = state,
                onNameChanged = onNameChanged,
                onColorSelected = onColorSelected,
            )

            1 -> GroupStep(
                groups = state.groups,
                groupsLoading = state.groupsLoading,
                selectedGroupId = state.selectedGroupId,
                onGroupSelected = onGroupSelected,
                onCreateGroupClicked = onCreateGroupClicked,
            )

            2 -> FrequencyStep(
                state = state,
                onFrequencySelected = onFrequencySelected,
            )

            3 -> ReminderTimeStep(
                state = state,
                onTimeSelected = onTimeSelected,
            )
        }

        if (state.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.error,
                fontSize = 14.sp,
                color = NekkoTheme.colors.red.default,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        FooterRow(
            currentStep = state.currentStep,
            canAdvance = state.canAdvanceFromStep,
            canSubmit = state.canSubmit,
            isSubmitting = state.isSubmitting,
            onBackStep = onBackStep,
            onNextStep = onNextStep,
            onSubmit = onSubmit,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepProgress(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(steps.size) { index ->
            val isFilled = index <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isFilled) NekkoTheme.colors.green.active
                        else NekkoTheme.colors.fill.tertiary
                    ),
            )
        }
    }
}

@Composable
private fun FooterRow(
    currentStep: Int,
    canAdvance: Boolean,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onBackStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentStep > 0) {
            FilledIconButton(
                modifier = Modifier.size(58.dp),
                onClick = onBackStep,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = NekkoTheme.colors.fill.tertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NekkoTheme.colors.text.primary,
                )
            }

            Spacer(Modifier.width(12.dp))
        }

        if (currentStep < steps.lastIndex) {
            NekkoButton(
                text = "Next",
                onClick = onNextStep,
                modifier = Modifier.weight(1f),
                enabled = canAdvance,
            )
        } else {
            NekkoButton(
                text = "Save Contact",
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                enabled = canSubmit,
                loading = isSubmitting,
            )
        }
    }
}

@Composable
private fun NameAndAvatarStep(
    state: AddContactState,
    onNameChanged: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Name",
                    color = NekkoTheme.colors.text.tertiary,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NekkoTheme.colors.fill.secondary,
                unfocusedContainerColor = NekkoTheme.colors.fill.secondary,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = NekkoTheme.colors.text.primary,
                focusedTextColor = NekkoTheme.colors.text.primary,
                unfocusedTextColor = NekkoTheme.colors.text.primary,
            ),
        )

        Spacer(Modifier.height(28.dp))

        SectionLabel("Avatar color")

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddContactViewModel.colorHexes.forEachIndexed { index, hex ->
                val isSelected = state.selectedColorIndex == index
                ColorCircle(
                    hex = hex,
                    isSelected = isSelected,
                    onClick = { onColorSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun GroupStep(
    groups: List<Group>,
    groupsLoading: Boolean,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
    onCreateGroupClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (groupsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NekkoTheme.colors.green.active)
            }
        } else {
            if (groups.isEmpty()) {
                Text(
                    text = "No groups yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    groups.forEach { group ->
                        GroupCell(
                            group = group,
                            isSelected = group.id == selectedGroupId,
                            onClick = { onGroupSelected(group.id) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(NekkoTheme.colors.fill.secondary)
                .clickable(onClick = onCreateGroupClicked)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = NekkoTheme.colors.green.active,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Create a new group",
                    style = NekkoTheme.typography.heading4Semibold,
                    color = NekkoTheme.colors.text.primary,
                )
            }
        }
    }
}

@Composable
private fun GroupCell(
    group: Group,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) NekkoTheme.colors.fill.primary
                else NekkoTheme.colors.fill.secondary
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(rememberColorFromHex(group.color ?: "#9E9E9E")),
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = group.name,
            style = NekkoTheme.typography.heading4Semibold,
            color = NekkoTheme.colors.text.primary,
            modifier = Modifier.weight(1f),
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NekkoTheme.colors.green.active,
            )
        }
    }
}

@Composable
private fun FrequencyStep(
    state: AddContactState,
    onFrequencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        frequencies.forEach { (value, label) ->
            FrequencyCard(
                label = label,
                isSelected = state.selectedFrequency == value,
                onClick = { onFrequencySelected(value) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReminderTimeStep(
    state: AddContactState,
    onTimeSelected: (Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TimePicker(
        hour = state.selectedHour,
        minute = state.selectedMinute,
        isAm = state.isAm,
        onTimeSelected = onTimeSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var groupName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
        ) {
            Text(
                text = "Create Group",
                style = NekkoTheme.typography.heading3,
                fontWeight = FontWeight.SemiBold,
                color = NekkoTheme.colors.text.primary,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "New Group",
                        color = NekkoTheme.colors.text.tertiary,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NekkoTheme.colors.fill.secondary,
                    unfocusedContainerColor = NekkoTheme.colors.fill.secondary,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = NekkoTheme.colors.green.active,
                    focusedTextColor = NekkoTheme.colors.text.primary,
                    unfocusedTextColor = NekkoTheme.colors.text.primary,
                ),
            )

            Spacer(Modifier.height(24.dp))

            NekkoButton(
                text = "Save",
                onClick = { onSave(groupName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank() && !isSaving,
                loading = isSaving,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = NekkoTheme.typography.heading3Bold,
        color = NekkoTheme.colors.text.primary,
    )
}

@Composable
private fun ColorCircle(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = rememberColorFromHex(hex)
    Box(
        modifier = modifier
            .size(if (isSelected) 48.dp else 40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, NekkoTheme.colors.text.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
    )
}

private fun rememberColorFromHex(hex: String): Color {
    val value = hex.removePrefix("#").toLongOrNull(16) ?: return Color.Gray
    return Color((0xFF000000L or value).toInt())
}

@Composable
private fun FrequencyCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) NekkoTheme.colors.fill.primary
                else NekkoTheme.colors.fill.secondary
            )
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = NekkoTheme.typography.heading4Semibold,
            color = NekkoTheme.colors.text.primary,
        )
    }
}

@Composable
private fun TimePicker(
    hour: Int,
    minute: Int,
    isAm: Boolean,
    onTimeSelected: (Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = NekkoTheme.colors.text.primary,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StepperColumn(
                values = (1..12).map { it.toString().padStart(2, '0') },
                selected = hour.toString().padStart(2, '0'),
                onSelect = { onTimeSelected(it.toInt(), minute, isAm) },
            )
            Text(
                text = ":",
                fontSize = 32.sp,
                color = NekkoTheme.colors.text.tertiary,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            StepperColumn(
                values = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).map {
                    it.toString().padStart(2, '0')
                },
                selected = minute.toString().padStart(2, '0'),
                onSelect = { onTimeSelected(hour, it.toInt(), isAm) },
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmPmPill(
                    label = "AM",
                    isSelected = isAm,
                    onClick = { onTimeSelected(hour, minute, true) },
                )
                AmPmPill(
                    label = "PM",
                    isSelected = !isAm,
                    onClick = { onTimeSelected(hour, minute, false) },
                )
            }
        }
    }
}

@Composable
private fun StepperColumn(
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(200.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { value ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) NekkoTheme.colors.fill.primary
                        else NekkoTheme.colors.fill.secondary
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = NekkoTheme.colors.text.primary,
                )
            }
        }
    }
}

@Composable
private fun AmPmPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) NekkoTheme.colors.fill.primary
                else NekkoTheme.colors.fill.secondary
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = NekkoTheme.colors.text.primary,
        )
    }
}
