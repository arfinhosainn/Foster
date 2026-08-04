package app.usenekko.home.presentation.settings

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.AmbientGlow
import app.usenekko.home.di.rememberGroupSettingsViewModel
import app.usenekko.home.domain.Group
import app.usenekko.home.presentation.settings.components.SettingsTopBar
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_add
import nekko.home.generated.resources.ic_edit
import nekko.home.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.vectorResource

@Composable
fun GroupSettingsScreen(
    onBack: () -> Unit,
    onGroupClick: (Group) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberGroupSettingsViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val liquidState = rememberLiquidState()
    var isEditOptions by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AmbientGlow(liquidState, Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 120.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isLoading) {
                Spacer(Modifier.height(16.dp))
                Text("Loading…", color = NekkoTheme.colors.text.tertiary)
            } else if (state.groups.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "No groups yet",
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap + to create your first group",
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 14.sp,
                )
            } else {
                state.groups.forEach { group ->
                    GroupRow(
                        name = group.name,
                        memberCount = state.memberCount(group.id),
                        editMode = isEditOptions,
                        onRemove = { viewModel.onAction(GroupSettingsAction.DeleteGroup(group.id)) },
                        onClick = { onGroupClick(group) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(error, color = Color(0xFFFF4B4B), fontSize = 13.sp)
            }
        }

        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            SettingsTopBar(
                onBack = onBack,
                title = "Groups",
                actions = {
                    IconButton(onClick = { viewModel.onAction(GroupSettingsAction.OpenCreateDialog) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_add),
                            contentDescription = "Create group",
                        )
                    }
                    IconButton(onClick = { isEditOptions = !isEditOptions }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_edit),
                            contentDescription = if (isEditOptions) "Done" else "Edit groups",
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
            .background(NekkoTheme.colors.background.b1, RoundedCornerShape(20.dp))
            .clickable(enabled = !editMode, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NekkoTheme.colors.fill.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = NekkoTheme.colors.text.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = NekkoTheme.colors.text.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$memberCount member${if (memberCount == 1) "" else "s"}",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 13.sp,
            )
        }
        if (editMode) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_trashbin),
                    contentDescription = "Remove group",
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
        title = { Text("New Group") },
        text = {
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftNameChange,
                label = { Text("Group name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = draftName.isNotBlank() && !isSaving) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
    )
}