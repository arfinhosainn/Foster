package app.usenekko.home.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.AmbientGlow
import app.usenekko.home.di.rememberAccountViewModel
import app.usenekko.home.presentation.badges.BadgeRow
import app.usenekko.home.presentation.components.avatarIndexForId
import app.usenekko.home.presentation.settings.components.SettingsTopBar
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberAccountViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val liquidState = rememberLiquidState()
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refreshIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(liquidState, Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
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
                        color = NekkoTheme.colors.text.tertiary,
                        strokeWidth = 1.5.dp,
                    )
                }
            }
            Spacer(Modifier.height(120.dp))

            AccountAvatar(
                selectedAvatarId = state.profile?.selectedAvatarId,
                onEditClick = { showAvatarPicker = true },
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = state.fullName ?: "—",
                color = NekkoTheme.colors.text.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = state.createdAt?.let { "Joined ${formatJoinedDate(it)}" } ?: "",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatCard(
                    label = "Contacts",
                    value = state.totalContacts.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Check-ins",
                    value = state.totalCheckIns.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.badgeSlots.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                BadgeRow(badges = state.badgeSlots)
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF4B4B),
                    fontSize = 13.sp,
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            SettingsTopBar(onBack = onBack, title = "Account")
        }
    }

    if (showAvatarPicker) {
        ChooseProfileAvatarBottomSheet(
            selectedAvatarIndex = avatarIndexForId(state.profile?.selectedAvatarId),
            onAvatarSelected = viewModel::selectAvatar,
            onDismiss = { showAvatarPicker = false },
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(NekkoTheme.colors.background.b1, RoundedCornerShape(20.dp))
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = NekkoTheme.colors.text.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = NekkoTheme.colors.text.tertiary,
            fontSize = 14.sp,
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
    val year = date[0]
    return "$month $year"
}