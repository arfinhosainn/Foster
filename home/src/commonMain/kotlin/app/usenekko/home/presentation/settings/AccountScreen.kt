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
import androidx.compose.material3.Text
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
import app.usenekko.home.di.rememberAccountViewModel
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
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(liquidState, Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))

            MonogramAvatar(name = state.fullName.orEmpty())

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
}

@Composable
private fun MonogramAvatar(name: String) {
    val initials = name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .size(84.dp)
            .background(NekkoTheme.colors.fill.secondary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = NekkoTheme.colors.text.primary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
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