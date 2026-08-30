package app.usefoster.home.presentation.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.usefoster.home.di.rememberGroupSettingsViewModel
import app.usefoster.home.presentation.settings.components.SettingsTopBar
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_add
import foster.home.generated.resources.ic_edit
import foster.home.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.action_cancel
import foster.home.generated.resources.action_create
import foster.home.generated.resources.action_done
import foster.home.generated.resources.add_new_group
import foster.home.generated.resources.cd_create_group
import foster.home.generated.resources.cd_edit_groups
import foster.home.generated.resources.cd_remove_group
import foster.home.generated.resources.group_members_many
import foster.home.generated.resources.group_members_one
import foster.home.generated.resources.group_name_label
import foster.home.generated.resources.settings_groups
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberGroupSettingsViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isEditOptions by rememberSaveable { mutableStateOf(false) }
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refreshIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(Modifier.matchParentSize().background(FosterTheme.colors.background.b0))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 120.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isRefreshing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = FosterTheme.colors.text.tertiary,
                        strokeWidth = 1.5.dp,
                    )
                }
            }
            if (state.isLoading) {
                GroupSettingsLoadingSkeleton()
            } else if (state.groups.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "No groups yet",
                    color = FosterTheme.colors.text.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap + to create your first group",
                    color = FosterTheme.colors.text.tertiary,
                    fontSize = 14.sp,
                )
            } else {
                state.groups.forEach { group ->
                    GroupRow(
                        name = group.name,
                        memberCount = state.memberCount(group.id),
                        editMode = isEditOptions,
                        onRemove = { viewModel.onAction(GroupSettingsAction.DeleteGroup(group.id)) },
                        onClick = { selectedGroupId = group.id },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(stringResource(error), color = Color(0xFFFF4B4B), fontSize = 13.sp)
            }
        }

        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            SettingsTopBar(
                onBack = onBack,
                title = stringResource(Res.string.settings_groups),
                actions = {
                    IconButton(onClick = { viewModel.onAction(GroupSettingsAction.OpenCreateDialog) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_add),
                            contentDescription = stringResource(Res.string.cd_create_group),
                        )
                    }
                    IconButton(onClick = { isEditOptions = !isEditOptions }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_edit),
                            contentDescription = if (isEditOptions) stringResource(Res.string.action_done) else stringResource(Res.string.cd_edit_groups),
                        )
                    }
                },
            )
        }
    }

    if (state.isCreateDialogOpen) {
        CreateGroupDialog(
            draftName = state.draftName,
            onDraftNameChange = { viewModel.onAction(GroupSettingsAction.DraftNameChanged(it)) },
            onConfirm = { viewModel.onAction(GroupSettingsAction.CreateGroup) },
            onDismiss = { viewModel.onAction(GroupSettingsAction.CloseCreateDialog) },
            isSaving = state.isSaving,
        )
    }

    selectedGroupId?.let { groupId ->
        GroupBottomSheet(
            groupId = groupId,
            onDismiss = { selectedGroupId = null },
        )
    }
}

@Composable
private fun GroupSettingsLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "groupSettingsShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "groupSettingsShimmerPosition",
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
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FosterTheme.colors.background.b1, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(shimmerBrush, CircleShape),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(if (rowIndex % 2 == 0) 132.dp else 104.dp)
                            .height(18.dp)
                            .background(shimmerBrush, RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(14.dp)
                            .background(shimmerBrush, RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    name: String,
    memberCount: Int,
    editMode: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FosterTheme.colors.background.b1, RoundedCornerShape(20.dp))
            .clickable(enabled = !editMode, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(FosterTheme.colors.fill.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = FosterTheme.colors.text.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = FosterTheme.colors.text.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (memberCount == 1) stringResource(Res.string.group_members_one, memberCount) else stringResource(Res.string.group_members_many, memberCount),
                color = FosterTheme.colors.text.tertiary,
                fontSize = 13.sp,
            )
        }
        if (editMode) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_trashbin),
                    contentDescription = stringResource(Res.string.cd_remove_group),
                    tint = Color(0xFFFF4B4B),
                )
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    draftName: String,
    onDraftNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_new_group)) },
        text = {
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftNameChange,
                label = { Text(stringResource(Res.string.group_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = draftName.isNotBlank() && !isSaving) {
                Text(stringResource(Res.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}