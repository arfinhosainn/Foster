package app.usenekko.home.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.home.domain.BadgeSlot
import app.usenekko.home.di.rememberAccountViewModel
import app.usenekko.home.presentation.badges.badgeIcon
import app.usenekko.home.presentation.components.avatarIndexForId
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.designsystem.avatar.ChooseAvatarBottomSheet
import app.usenekko.designsystem.avatar.avatarResources
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_close
import nekko.home.generated.resources.ic_contacts
import nekko.home.generated.resources.ic_lock
import nekko.home.generated.resources.ic_sprout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.cd_close
import nekko.home.generated.resources.cd_locked
import nekko.home.generated.resources.settings_account
import nekko.home.generated.resources.settings_check_ins_stat
import nekko.home.generated.resources.settings_contacts
import nekko.home.generated.resources.settings_joined
import nekko.home.generated.resources.settings_saving_avatar
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheet(
    onDismiss: () -> Unit,
) {
    val viewModel = rememberAccountViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle(
                    color = NekkoTheme.colors.gray.quaternary,
                    modifier = Modifier.align(Alignment.Center),
                )
                CloseButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, top = 5.dp),
                )
            }
        },
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.settings_account),
                    style = NekkoTheme.typography.heading3,
                    fontWeight = FontWeight.SemiBold,
                    color = NekkoTheme.colors.text.primary,
                )
            }

            Spacer(Modifier.height(28.dp))
            AccountAvatar(
                selectedAvatarId = state.profile?.selectedAvatarId,
                onEditClick = { showAvatarPicker = true },
                modifier = Modifier.size(120.dp),
            )
            if (state.isUpdatingAvatar) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.settings_saving_avatar),
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(32.dp))

            Text(
                text = state.fullName ?: "—",
                color = NekkoTheme.colors.text.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.createdAt?.let { stringResource(Res.string.settings_joined, formatJoinedDate(it)) } ?: "",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NekkoTheme.colors.fill.tertiary),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountStat(
                    value = state.totalCheckIns.toString(),
                    label = stringResource(Res.string.settings_check_ins_stat),
                    icon = Res.drawable.ic_sprout,
                    iconTint = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(NekkoTheme.colors.stroke.primary),
                )
                AccountStat(
                    value = state.totalContacts.toString(),
                    label = stringResource(Res.string.settings_contacts),
                    icon = Res.drawable.ic_contacts,
                    iconTint = Color(0xFFFFCC33),
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.badgeSlots.isNotEmpty()) {
                Spacer(Modifier.height(40.dp))
                AccountBadgeRow(state.badgeSlots)
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF4B4B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            }
        }
    }

    if (showAvatarPicker) {
        ChooseAvatarBottomSheet(
            selectedAvatarIndex = avatarIndexForId(state.profile?.selectedAvatarId),
            onAvatarSelected = viewModel::selectAvatar,
            onDismiss = { showAvatarPicker = false },
        )
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = NekkoTheme.colors.gray.secondary,
        ),
    ) {
        Icon(imageVector = vectorResource(Res.drawable.ic_close), contentDescription = stringResource(Res.string.cd_close),
            modifier = Modifier.size(24.dp))
    }
}


@Composable
private fun AccountStat(
    value: String,
    label: String,
    icon: DrawableResource,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NekkoTheme.colors.fill.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(icon),
                contentDescription = null,
                tint = iconTint,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = NekkoTheme.colors.text.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AccountBadgeRow(badges: List<BadgeSlot>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            badges.forEach { slot ->
                AccountBadgeItem(slot)
            }
        }
    }
}

@Composable
private fun AccountBadgeItem(slot: BadgeSlot) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NekkoTheme.colors.background.b2)
                .border(1.dp, NekkoTheme.colors.stroke.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(badgeIcon(slot.badge)),
                contentDescription = if (slot.unlocked) slot.badge.name else null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-8).dp)
                    .scale(1.6f)
                    .then(
                        if (slot.unlocked) {
                            Modifier
                        } else {
                            Modifier.blur(
                                radius = 14.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded,
                            )
                        },
                    ),
                alpha = if (slot.unlocked) 1f else 1f,
            )
            if (!slot.unlocked) {
                Image(
                    painter = painterResource(Res.drawable.ic_lock),
                    contentDescription = stringResource(Res.string.cd_locked),
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.62f)),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (slot.unlocked) slot.badge.name else "Locked",
            color = if (slot.unlocked) NekkoTheme.colors.text.primary else NekkoTheme.colors.text.tertiary,
            fontSize = 15.sp,
            fontWeight = if (slot.unlocked) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatJoinedDate(iso: String): String {
    val date = iso.substringBefore('T').split("-")
    if (date.size != 3) return iso.substringBefore('T')
    val month = when (date[1]) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; else -> "Dec"
    }
    val day = date[2].toIntOrNull() ?: return iso.substringBefore('T')
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix $month, ${date[0]}"
}