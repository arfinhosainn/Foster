package app.usefoster.home.addcontact

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.usefoster.designsystem.buttons.FosterActionButton
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.designsystem.shapes.SawToothCircleShape
import app.usefoster.designsystem.buttons.AmPmToggle
import app.usefoster.designsystem.timepicker.TimeScrollDial
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.Group
import app.usefoster.home.domain.GroupMembership
import app.usefoster.home.presentation.components.ContactAvatar
import app.usefoster.home.presentation.components.contactsForGroup
import app.usefoster.home.presentation.components.groupMemberCount
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.shared.contacts.rememberContactPicker
import app.usefoster.designsystem.avatar.ChooseAvatarBottomSheet
import app.usefoster.designsystem.avatar.ProfilePhotoPicker
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_add
import foster.home.generated.resources.ic_back
import foster.home.generated.resources.ic_circlecheck
import foster.home.generated.resources.ic_close
import foster.home.generated.resources.ic_forward
import foster.home.generated.resources.ic_import
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.action_next
import foster.home.generated.resources.action_save
import foster.home.generated.resources.add_contact_name_label
import foster.home.generated.resources.add_create_group
import foster.home.generated.resources.add_freq_annually
import foster.home.generated.resources.add_freq_biweekly
import foster.home.generated.resources.add_freq_daily
import foster.home.generated.resources.add_freq_monthly
import foster.home.generated.resources.add_freq_semiannually
import foster.home.generated.resources.add_freq_weekly
import foster.home.generated.resources.add_import_contact
import foster.home.generated.resources.add_new_group
import foster.home.generated.resources.add_no_groups_yet
import foster.home.generated.resources.add_save_contact
import foster.home.generated.resources.add_step_frequency_subtitle
import foster.home.generated.resources.add_step_frequency_title
import foster.home.generated.resources.add_step_group_subtitle
import foster.home.generated.resources.add_step_group_title
import foster.home.generated.resources.add_step_new_contact_subtitle
import foster.home.generated.resources.add_step_new_contact_title
import foster.home.generated.resources.add_step_time_subtitle
import foster.home.generated.resources.add_step_time_title
import foster.home.generated.resources.add_tap_plus_button
import foster.home.generated.resources.add_wanna_create_group
import foster.home.generated.resources.cd_add_members
import foster.home.generated.resources.cd_back
import foster.home.generated.resources.cd_close
import foster.home.generated.resources.cd_edit_profile_picture
import foster.home.generated.resources.cd_selected
import foster.home.generated.resources.edit_contact_subtitle
import foster.home.generated.resources.edit_contact_title
import org.jetbrains.compose.resources.stringResource

private val frequencies = listOf(
    "daily" to Res.string.add_freq_daily,
    "weekly" to Res.string.add_freq_weekly,
    "biweekly" to Res.string.add_freq_biweekly,
    "monthly" to Res.string.add_freq_monthly,
    "semiannually" to Res.string.add_freq_semiannually,
    "annually" to Res.string.add_freq_annually
)


private data class StepMeta(
    val titleRes: StringResource,
    val subtitleRes: StringResource,
)

private val steps = listOf(
    StepMeta(Res.string.add_step_new_contact_title, Res.string.add_step_new_contact_subtitle),
    StepMeta(Res.string.add_step_group_title, Res.string.add_step_group_subtitle),
    StepMeta(Res.string.add_step_frequency_title, Res.string.add_step_frequency_subtitle),
    StepMeta(Res.string.add_step_time_title, Res.string.add_step_time_subtitle),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onShowPaywall: () -> Unit = {},
    onShowDiscountPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launchContactPicker = rememberContactPicker(
        onContactSelected = viewModel::onContactImported,
        onPermissionDenied = { },
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddContactEvent.Saved -> onSaved()
                is AddContactEvent.ShowPaywall -> {
                    onDismiss()
                    if (event.showDiscount) {
                        onShowDiscountPaywall()
                    } else {
                        onShowPaywall()
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FosterTheme.colors.background.b0,
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
                        .wrapContentSize()
                        .clip(CircleShape)
                        .background(Color.Unspecified)
                        .clickable {
                            viewModel.resetDraft()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = FosterTheme.colors.text.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        AdaptiveSurface {
            AddContactSheetContent(
                state = state,
                isEditing = state.isEditing,
                onNameChanged = viewModel::onNameChanged,
                onAvatarSelected = viewModel::onAvatarSelected,
                onGroupSelected = viewModel::onGroupSelected,
                onCreateGroupClicked = viewModel::onCreateGroupClicked,
                onFrequencySelected = viewModel::onFrequencySelected,
                onTimeSelected = viewModel::onTimeSelected,
                onTimeDialChanged = viewModel::onTimeDialChanged,
                onBackStep = viewModel::onBackStep,
                onNextStep = viewModel::onNextStep,
                onSubmit = viewModel::submit,
                onImportContact = launchContactPicker,
            )
        }
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
    isEditing: Boolean,
    onNameChanged: (String) -> Unit,
    onAvatarSelected: (Int) -> Unit,
    onGroupSelected: (String) -> Unit,
    onCreateGroupClicked: () -> Unit,
    onFrequencySelected: (String) -> Unit,
    onTimeSelected: (Int, Int, Boolean) -> Unit,
    onTimeDialChanged: (Int) -> Unit,
    onBackStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmit: () -> Unit,
    onImportContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepMeta = steps[state.currentStep].let {
        if (isEditing && state.currentStep == 0) {
            StepMeta(Res.string.edit_contact_title, Res.string.edit_contact_subtitle)
        } else {
            it
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {

        Text(
            text = stringResource(stepMeta.titleRes),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center,
            color = FosterTheme.colors.text.primary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(stepMeta.subtitleRes),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = FosterTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(40.dp))

        when (state.currentStep) {
            0 -> NameAndAvatarStep(
                state = state,
                onNameChanged = onNameChanged,
                onAvatarSelected = onAvatarSelected,
                onImportContact = onImportContact,
            )

            1 -> GroupStep(
                groups = state.groups,
                contacts = state.contacts,
                memberships = state.memberships,
                groupsLoading = state.groupsLoading,
                selectedGroupId = state.selectedGroupId,
                pendingAvatarColor = AddContactViewModel.colorHexes[state.selectedAvatarIndex ?: 0],
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
                onTimeDialChanged = onTimeDialChanged,
            )
        }

        if (state.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(state.error),
                fontSize = 14.sp,
                color = FosterTheme.colors.red.default,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(48.dp))

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
            FosterActionButton(
                onClick = onBackStep,
                leadingIcon = vectorResource(Res.drawable.ic_back),
            )

            Spacer(Modifier.width(12.dp))
        }

        if (currentStep < steps.lastIndex) {
            FosterButton(
                text = stringResource(Res.string.action_next),
                onClick = onNextStep,
                modifier = Modifier.weight(1f),
                enabled = canAdvance,
            )
        } else {
            FosterButton(
                text = stringResource(Res.string.add_save_contact),
                onClick = onSubmit,
                modifier = Modifier.weight(1f).height(58.dp),
                enabled = canSubmit,
                loading = isSubmitting,
            )
        }
    }
}

@Composable
internal fun NameAndAvatarStep(
    state: AddContactState,
    onNameChanged: (String) -> Unit,
    onAvatarSelected: (Int) -> Unit,
    onImportContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfilePhotoPicker(
            photoBitmap = state.importedPhoto,
            avatarSize = 130.dp,
            selectedAvatarIndex = state.selectedAvatarIndex,
            onEditClick = { showAvatarPicker = true },
        )

        Spacer(Modifier.height(40.dp))

        StepFieldContainer {
            Box(modifier = Modifier.weight(1f)) {
                if (state.name.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.add_contact_name_label),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FosterTheme.colors.text.tertiary,
                    )
                }
                BasicTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FosterTheme.typography.bodyMedium.fontFamily,
                        color = FosterTheme.colors.text.primary,
                    ),
                    cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onImportContact)
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                imageVector = vectorResource(Res.drawable.ic_import),
                contentDescription = null,
            )
            Spacer(Modifier.width(13.dp))
            Text(
                text = stringResource(Res.string.add_import_contact),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = FosterTheme.colors.text.secondary,
            )
        }
    }

    if (showAvatarPicker) {
        ChooseAvatarBottomSheet(
            selectedAvatarIndex = state.selectedAvatarIndex,
            onAvatarSelected = onAvatarSelected,
            onDismiss = { showAvatarPicker = false },
        )
    }
}

@Composable
private fun StepFieldContainer(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = vectorResource(Res.drawable.ic_forward),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(FosterTheme.colors.fill.secondary)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}


@Composable
internal fun GroupStep(
    groups: List<Group>,
    contacts: List<Contact>,
    memberships: List<GroupMembership>,
    groupsLoading: Boolean,
    selectedGroupId: String?,
    pendingAvatarColor: String,
    onGroupSelected: (String) -> Unit,
    onCreateGroupClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (groupsLoading) {
            GroupPickerLoadingSkeleton()
        } else if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.add_no_groups_yet),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = FosterTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            GroupGrid(
                groups = groups,
                contacts = contacts,
                memberships = memberships,
                selectedGroupId = selectedGroupId,
                pendingAvatarColor = pendingAvatarColor,
                onGroupClick = onGroupSelected,
            )
        }

        Spacer(Modifier.height(40.dp))

        FosterActionButton(
            onClick = onCreateGroupClicked,
            leadingIcon = vectorResource(Res.drawable.ic_add),
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(Res.string.add_wanna_create_group),
            color = FosterTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.add_tap_plus_button),
            color = FosterTheme.colors.text.tertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GroupPickerLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "addContactGroupShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "addContactGroupShimmerPosition",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            FosterTheme.colors.fill.quaternary,
            FosterTheme.colors.fill.secondary,
            FosterTheme.colors.fill.quaternary,
        ),
        start = Offset(shimmerPosition * 500f, 0f),
        end = Offset((shimmerPosition + 1f) * 500f, 500f),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(2) {
                    GroupPickerItemLoadingSkeleton(
                        brush = shimmerBrush,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupPickerItemLoadingSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SawToothCircleShape())
                .background(brush),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush),
        )
    }
}

@Composable
private fun GroupGrid(
    groups: List<Group>,
    contacts: List<Contact>,
    memberships: List<GroupMembership>,
    selectedGroupId: String?,
    pendingAvatarColor: String,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(28.dp),
) {
    groups.chunked(2).forEach { rowGroups ->
        val isShortRow = rowGroups.size < 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Half a cell of padding on each side centers a lone trailing item.
            if (isShortRow) Spacer(Modifier.weight(0.5f))

            rowGroups.forEach { group ->
                GroupCell(
                    group = group,
                    members = contactsForGroup(group.id, contacts, memberships),
                    memberCount = groupMemberCount(group.id, memberships),
                    isSelected = group.id == selectedGroupId,
                    pendingAvatarColor = pendingAvatarColor,
                    onClick = { onGroupClick(group.id) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (isShortRow) Spacer(Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun GroupCell(
    group: Group,
    members: List<Contact>,
    memberCount: Int,
    isSelected: Boolean,
    pendingAvatarColor: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    GroupCard(
        group = group,
        members = members,
        isSelected = isSelected,
        pendingAvatarColor = pendingAvatarColor,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = group.name,
        style = FosterTheme.typography.heading4Semibold,
        color = FosterTheme.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(2.dp))

    Text(
        text = "$memberCount ${if (memberCount == 1) "person" else "people"}",
        style = FosterTheme.typography.footnote,
        color = FosterTheme.colors.text.tertiary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GroupCard(
    group: Group,
    members: List<Contact>,
    isSelected: Boolean,
    pendingAvatarColor: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupColor = rememberColorFromHex(group.color ?: "#9E9E9E")
    val avatarRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
    )
    Box(
        modifier = modifier
            .clip(SawToothCircleShape())
            .background(FosterTheme.colors.fill.secondary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            // Selected: the contact being added appears in the middle, like the onboarding group screen.
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(FosterTheme.colors.background.b2)
                    .border(2.dp, avatarRingBrush, CircleShape)
                    .padding(3.dp),
            ) {
                ContactAvatar(
                    avatarColor = pendingAvatarColor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else if (members.isEmpty() && group.color == null) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.cd_add_members),
                tint = FosterTheme.colors.text.quaternary,
                modifier = Modifier.size(40.dp),
            )
        } else {
            if (members.isEmpty()) {
                SingleMemberAvatar(color = groupColor)
            } else {
                GroupMemberAvatarStack(members = members)
            }
        }
    }
}

@Composable
private fun GroupMemberAvatarStack(
    members: List<Contact>,
    modifier: Modifier = Modifier,
) {

    val avatarRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
    )
    val visibleMembers = members.take(2)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        visibleMembers.forEachIndexed { index, contact ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = if (visibleMembers.size == 2) (index * 28 - 14).dp else 0.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(FosterTheme.colors.background.b2)
                    .border(2.dp, avatarRingBrush, CircleShape)
                    .padding(3.dp),
            ) {
                ContactAvatar(
                    avatarColor = contact.avatarColor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SingleMemberAvatar(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FosterTheme.colors.background.b2),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

@Composable
internal fun FrequencyStep(
    state: AddContactState,
    onFrequencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        frequencies.forEach { (value, labelRes) ->
            ReminderOptionCard(
                text = stringResource(labelRes),
                isSelected = state.selectedFrequency == value,
                onClick = { onFrequencySelected(value) },
            )
        }
    }
}

@Composable
internal fun ReminderTimeStep(
    state: AddContactState,
    onTimeSelected: (Int, Int, Boolean) -> Unit,
    onTimeDialChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimeScrollDial(
            totalMinutes = if (state.selectedHour == 12) {
                state.selectedMinute
            } else {
                state.selectedHour * 60 + state.selectedMinute
            },
            onValueChange = onTimeDialChanged,
            modifier = modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        AmPmToggle(
            isAm = state.isAm,
            onToggle = { isAm ->
                onTimeSelected(state.selectedHour, state.selectedMinute, isAm)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateGroupSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var groupName by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FosterTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = FosterTheme.colors.gray.quaternary) },
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
                    .imePadding(),
            ) {
            Text(
                text = stringResource(Res.string.add_create_group),
                style = FosterTheme.typography.heading3,
                fontWeight = FontWeight.SemiBold,
                color = FosterTheme.colors.text.primary,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.add_new_group),
                        color = FosterTheme.colors.text.tertiary,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FosterTheme.colors.fill.secondary,
                    unfocusedContainerColor = FosterTheme.colors.fill.secondary,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = FosterTheme.colors.green.active,
                    focusedTextColor = FosterTheme.colors.text.primary,
                    unfocusedTextColor = FosterTheme.colors.text.primary,
                ),
            )

            Spacer(Modifier.height(24.dp))

            FosterButton(
                text = stringResource(Res.string.action_save),
                onClick = { onSave(groupName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank() && !isSaving,
                loading = isSaving,
            )
            }
        }
    }
}

private fun rememberColorFromHex(hex: String): Color {
    val value = hex.removePrefix("#").toLongOrNull(16) ?: return Color.Gray
    return Color((0xFF000000L or value).toInt())
}

@Composable
private fun ReminderOptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(FosterTheme.colors.fill.secondary)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_circlecheck),
                    contentDescription = stringResource(Res.string.cd_selected),
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = FosterTheme.typography.heading4Semibold,
                color = FosterTheme.colors.text.primary,
            )
        }
    }
}
