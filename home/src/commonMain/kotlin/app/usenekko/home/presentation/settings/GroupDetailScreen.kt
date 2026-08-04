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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.AmbientGlow
import app.usenekko.home.di.rememberGroupDetailViewModel
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.presentation.settings.components.SettingsTopBar
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_edit
import nekko.home.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.vectorResource

@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberGroupDetailViewModel(groupId)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val liquidState = rememberLiquidState()

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
            } else if (state.members.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "No members yet",
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Members can be added from the Home screen group filter",
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 14.sp,
                )
            } else {
                state.members.forEach { member ->
                    MemberRow(
                        contact = member,
                        onMove = { viewModel.onAction(GroupDetailAction.OpenMoveDialog(member)) },
                        onRemove = {
                            viewModel.onAction(GroupDetailAction.RemoveMember(member.id))
                        },
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
            SettingsTopBar(onBack = onBack, title = state.groupName.ifEmpty { "Group" })
        }
    }

    if (state.isMoveDialogOpen) {
        MoveMemberDialog(
            contact = state.movingContact,
            otherGroups = state.otherGroups,
            onSelect = { toGroupId ->
                state.movingContact?.let {
                    viewModel.onAction(GroupDetailAction.MoveMember(it.id, toGroupId))
                }
            },
            onDismiss = { viewModel.onAction(GroupDetailAction.CloseMoveDialog) },
        )
    }
}

@Composable
private fun MemberRow(
    contact: Contact,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NekkoTheme.colors.background.b1, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(contactColor(contact.avatarColor, NekkoTheme.colors.fill.secondary), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = contact.name,
            color = NekkoTheme.colors.text.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMove) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_edit),
                contentDescription = "Move to another group",
                tint = NekkoTheme.colors.text.secondary,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_trashbin),
                contentDescription = "Remove from group",
                tint = Color(0xFFFF4B4B),
            )
        }
    }
}

@Composable
private fun MoveMemberDialog(
    contact: Contact?,
    otherGroups: List<Group>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move ${contact?.name ?: ""} to…") },
        text = {
            if (otherGroups.isEmpty()) {
                Text(
                    "No other groups yet — create one from the Groups screen.",
                    color = NekkoTheme.colors.text.tertiary,
                )
            } else {
                Column {
                    otherGroups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(group.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(NekkoTheme.colors.fill.secondary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = group.name.take(1).uppercase(),
                                    color = NekkoTheme.colors.text.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = group.name,
                                color = NekkoTheme.colors.text.primary,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun contactColor(color: String?, fallback: Color): Color {
    val value = color?.removePrefix("#")?.toLongOrNull(16) ?: return fallback
    return Color((0xFF000000L or value).toInt())
}