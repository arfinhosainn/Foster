package app.usenekko.home.presentation.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.shapes.SawToothCircleShape
import app.usenekko.home.di.rememberGroupSettingsViewModel
import app.usenekko.home.di.rememberGroupDetailViewModel
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.presentation.components.ContactAvatar
import app.usenekko.home.presentation.components.contactsForGroup
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_add
import nekko.home.generated.resources.ic_edit
import nekko.home.generated.resources.group_card
import nekko.home.generated.resources.ic_minus
import nekko.home.generated.resources.ic_trashbin
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import app.usenekko.home.presentation.contactprofile.DashedDivider
import nekko.home.generated.resources.ic_close
import nekko.home.generated.resources.ic_move
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupBottomSheet(
    groupId: String? = null,
    onDismiss: () -> Unit,
) {
    val viewModel = rememberGroupSettingsViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,)
    var selectedGroupId by rememberSaveable(groupId) { mutableStateOf(groupId) }





    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onAction(GroupSettingsAction.CancelEditing)
            onDismiss()
        },
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
                        .padding(end = 18.dp, top = 10.dp )
                        .clip(CircleShape)
                        .background(Color.Unspecified)
                        .clickable {
                            viewModel.onAction(GroupSettingsAction.CancelEditing)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = "Close",
                        tint = NekkoTheme.colors.gray.secondary,
                    )
                }
            }
        },
    ) {
        AdaptiveSurface {
            selectedGroupId?.let { groupId ->
                GroupMembersContent(
                    groupId = groupId,
                    onBack = { selectedGroupId = null },
                )
            } ?: run {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                Text(
                    text = "Groups",
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                when {
                    state.isLoading -> GroupGridLoadingSkeleton()

                    state.groups.isEmpty() -> Text(
                        text = "No groups yet",
                        color = NekkoTheme.colors.text.tertiary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    )

                    else -> GroupGrid(
                        state = state,
                        onGroupNameChanged = { groupId, name ->
                            viewModel.onAction(GroupSettingsAction.DraftGroupNameChanged(groupId, name))
                        },
                        onDelete = { groupId ->
                            viewModel.onAction(GroupSettingsAction.DeleteGroup(groupId))
                        },
                        onGroupClick = { selectedGroupId = it },
                    )
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = NekkoTheme.colors.red.default,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(35.dp))

                NekkoButton(
                    text = if (state.isEditing) "Save" else "Edit",
                    onClick = {
                        viewModel.onAction(
                            if (state.isEditing) GroupSettingsAction.SaveChanges
                            else GroupSettingsAction.StartEditing,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    loading = state.isSaving,
                )
                }
            }
        }
    }
}

@Composable
private fun GroupMembersContent(
    groupId: String,
    onBack: () -> Unit,
) {
    val viewModel = rememberGroupDetailViewModel(groupId)
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.groupName.ifEmpty { "Group" },
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${state.members.size} ${if (state.members.size == 1) "person" else "people"}",
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(Res.drawable.group_card),
            contentDescription = "Group members",
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        DashedDivider()

        Spacer(Modifier.height(20.dp))

        if (state.isRefreshing || state.isMutating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = NekkoTheme.colors.text.tertiary,
                    strokeWidth = 1.5.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (state.isMutating) "Updating members…" else "Refreshing…",
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 12.sp,
                )
            }
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NekkoTheme.colors.green.active)
            }

            state.members.isEmpty() -> Text(
                text = "No members in this group",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
            )

            else -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.members.forEach { contact ->
                    SwipeableMemberRow(
                        contact = contact,
                        checkInCount = state.checkInCounts[contact.id] ?: 0,
                        onMove = {
                            viewModel.onAction(GroupDetailAction.OpenMoveDialog(contact))
                        },
                        onRemove = {
                            viewModel.onAction(GroupDetailAction.RemoveMember(contact.id))
                        },
                    )
                }
            }
        }

        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                color = NekkoTheme.colors.red.default,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        NekkoButton(
            text = "Done",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
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
private fun SwipeableMemberRow(
    contact: Contact,
    checkInCount: Int,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 154.dp
    val swipeGap = 8.dp
    val swipeDistancePx = with(density) { (actionWidth + swipeGap).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(77.dp)
            .clip(RoundedCornerShape(24.dp))
            .clipToBounds(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            modifier = Modifier
                .width(actionWidth)
                .height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MemberActionButton(
                text = "Move",
                icon = Res.drawable.ic_move,
                tint = NekkoTheme.colors.gray.tertiary,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onMove()
                },
                backgroundColor = NekkoTheme.colors.fill.quaternary,
                modifier = Modifier.weight(1f),
            )
            MemberActionButton(
                text = "Remove",
                icon = Res.drawable.ic_trashbin,
                tint = NekkoTheme.colors.red.hover,
                onClick = {
                    scope.launch { offset.animateTo(0f) }
                    onRemove()
                },
                backgroundColor = NekkoTheme.colors.red.active,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .graphicsLayer { translationX = offset.value }
                .clip(RoundedCornerShape(24.dp))
                .background(NekkoTheme.colors.background.b1)
                .background(NekkoTheme.colors.fill.tertiary)
                .pointerInput(contact.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-swipeDistancePx, 0f))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                offset.animateTo(
                                    if (offset.value < -swipeDistancePx / 2f) -swipeDistancePx else 0f,
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactAvatar(
                avatarColor = contact.avatarColor,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(44.dp)
                    .border(1.5.dp, NekkoTheme.colors.stroke.secondary, CircleShape),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = checkInCountLabel(checkInCount),
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun MemberActionButton(
    text: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    tint: Color,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
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
                    "No other groups yet.",
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

private fun checkInCountLabel(count: Int): String = when (count) {
    1 -> "1 check-in"
    else -> "$count check-ins"
}

@Composable
private fun GroupGrid(
    state: GroupSettingsState,
    onGroupNameChanged: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onGroupClick: (String) -> Unit,
) {
    BoxWithConstraints {
        val columns = (maxWidth / 180.dp).toInt().coerceIn(2, 4)

        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            state.groups.chunked(columns).forEach { rowGroups ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowGroups.forEach { group ->
                        GroupItem(
                            group = group,
                            members = contactsForGroup(group.id, state.contacts, state.memberships),
                            memberCount = state.memberCount(group.id),
                            isEditing = state.isEditing,
                            name = state.draftNames[group.id] ?: group.name,
                            onNameChanged = { onGroupNameChanged(group.id, it) },
                            onDelete = { onDelete(group.id) },
                            onClick = { onGroupClick(group.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowGroups.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupGridLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "groupGridShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "groupGridShimmerPosition",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            NekkoTheme.colors.fill.quaternary,
            NekkoTheme.colors.fill.secondary,
            NekkoTheme.colors.fill.quaternary,
        ),
        start = Offset(shimmerPosition * 500f, 0f),
        end = Offset((shimmerPosition + 1f) * 500f, 500f),
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = (maxWidth / 180.dp).toInt().coerceIn(2, 4)

        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    repeat(columns) {
                        GroupItemLoadingSkeleton(
                            brush = shimmerBrush,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupItemLoadingSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(159.dp)
                .clip(SawToothCircleShape())
                .background(brush),
        )
        Spacer(Modifier.height(10.dp))
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
private fun GroupItem(
    group: Group,
    members: List<Contact>,
    memberCount: Int,
    isEditing: Boolean,
    name: String,
    onNameChanged: (String) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(159.dp),
            contentAlignment = Alignment.Center,
        ) {
            GroupSawToothCircle(
                group = group,
                members = members,
                memberCount = memberCount,
                modifier = Modifier.clickable(enabled = !isEditing, onClick = onClick),
            )
            if (isEditing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.background.b1)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_minus),
                        contentDescription = "Delete ${group.name}",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isEditing) {
            androidx.compose.foundation.text.BasicTextField(
                value = name,
                onValueChange = onNameChanged,
                singleLine = true,
                textStyle = NekkoTheme.typography.heading4Semibold.copy(
                    color = NekkoTheme.colors.text.primary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = name,
                style = NekkoTheme.typography.heading4Semibold,
                color = NekkoTheme.colors.text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "$memberCount ${if (memberCount == 1) "person" else "people"}",
            style = NekkoTheme.typography.footnote,
            color = NekkoTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GroupSawToothCircle(
    group: Group,
    members: List<Contact>,
    memberCount: Int,
    modifier: Modifier = Modifier,
) {
    val groupColor = rememberColorFromHex(group.color ?: "#9E9E9E")
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(SawToothCircleShape())
            .background(NekkoTheme.colors.fill.secondary),
        contentAlignment = Alignment.Center,
    ) {
        if (members.isEmpty() && group.color == null) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_add),
                contentDescription = null,
                tint = NekkoTheme.colors.text.quaternary,
                modifier = Modifier.size(40.dp),
            )
        } else if (members.isEmpty()) {
            SingleMemberAvatar(color = groupColor)
        } else {
            GroupMemberAvatarStack(members = members, memberCount = memberCount)
        }
    }
}

@Composable
private fun GroupMemberAvatarStack(
    members: List<Contact>,
    memberCount: Int,
) {
    val avatarRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
    )
    val visibleMembers = members.take(if (memberCount > 6) 4 else 2)
    Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
        visibleMembers.forEachIndexed { index, contact ->
            val isFourAvatarStack = visibleMembers.size == 4
            val xOffset = if (isFourAvatarStack) {
                if (index % 2 == 0) -24 else 24
            } else if (visibleMembers.size == 2) {
                index * 25 - 12
            } else {
                0
            }
            val yOffset = if (isFourAvatarStack) {
                if (index < 2) -24 else 24
            } else {
                0
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = xOffset.dp, y = yOffset.dp)
                    .size(49.dp)
                    .clip(CircleShape)
                    .background(NekkoTheme.colors.background.b2)
                    .border(2.dp, avatarRingBrush, CircleShape)
                    .padding(3.dp),
            ) {
                ContactAvatar(avatarColor = contact.avatarColor, modifier = Modifier.size(43.dp))
            }
        }
    }
}

@Composable
private fun SingleMemberAvatar(color: Color) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(NekkoTheme.colors.background.b2),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color))
            }
        }
    }
}

private fun rememberColorFromHex(hex: String): Color {
    val value = hex.removePrefix("#").toLongOrNull(16) ?: return Color.Gray
    return Color((0xFF000000L or value).toInt())
}