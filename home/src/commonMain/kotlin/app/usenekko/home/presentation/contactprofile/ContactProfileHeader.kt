package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.avatar_red
import nekko.home.generated.resources.ic_dropdown
import nekko.home.generated.resources.ic_notification
import nekko.home.generated.resources.ic_reminder
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ContactProfileHeader(
    name: String,
    frequencyLabel: String,
    reminderTime: String,
    isExpanded: Boolean,
    onNameClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onCheckInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectionRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33))
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(2.dp, selectionRingBrush, CircleShape)
                .background(NekkoTheme.colors.fill.secondary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                imageVector = vectorResource(Res.drawable.avatar_red), // TODO: map contact.avatarColor -> real avatar resource
                contentDescription = "Avatar",
                modifier = Modifier.size(64.dp)
            )
        }
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNameClick,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = NekkoTheme.colors.text.primary)
            Icon(
                imageVector = vectorResource(Res.drawable.ic_dropdown),
                contentDescription = if (isExpanded) "Collapse relationship info" else "Expand relationship info",
                tint = Color(0xFFE24B4A),
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        ContactCadenceRow(frequencyLabel = frequencyLabel, reminderTime = reminderTime)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            NekkoActionButton(
                text = "",
                leadingIcon = vectorResource(Res.drawable.ic_notification),
                onClick = onNotificationClick,
            )
            Spacer(Modifier.width(10.dp))
            NekkoButton(
                "Check-In",
                onClick = onCheckInClick,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}


@Composable
fun ContactCadenceRow(
    frequencyLabel: String,
    reminderTime: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_reminder), // placeholder — swap for the real icon later
            contentDescription = null,
            tint = NekkoTheme.colors.text.tertiary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "$frequencyLabel • $reminderTime",
            fontSize = 14.sp,
            color = NekkoTheme.colors.text.tertiary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}


@PreviewLightDark
@Composable
fun PreviewContactProfileHeader() {
    NekkoTheme {
        Surface {
            ContactProfileHeader(
                name = "Liam Hemsworth",
                frequencyLabel = "Bi-Weekly",
                reminderTime = "7:30",
                isExpanded = true,
                onNameClick = {},
                onNotificationClick = {},
                onCheckInClick = {},
            )
        }
    }
}
