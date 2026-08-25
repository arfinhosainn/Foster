package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.home.presentation.components.ContactAvatar
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_dropdown
import nekko.home.generated.resources.ic_notification
import nekko.home.generated.resources.ic_reminder
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ContactProfileHeader(
    name: String,
    avatarColor: String?,
    frequencyLabel: String,
    reminderTime: String,
    isExpanded: Boolean,
    daysUntilNextCheckIn: Int,
    ringProgress: Float,
    onNameClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onCheckInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            CheckInDreamBubble(
                days = daysUntilNextCheckIn,
                modifier = Modifier.zIndex(1f), // keep the bubble drawn on top of the avatar
            )

            Box(
                modifier = Modifier
                    .offset(y = 50.dp)
                    .size(82.dp),
                contentAlignment = Alignment.Center,
            ) {
                val trackColor = NekkoTheme.colors.fill.tertiary
                val arcBrush = Brush.sweepGradient(
                    0f to Color(0xFF34C759),
                    0.55f to Color(0xFFFFCC33),
                    1f to Color(0xFF34C759),
                )
                Canvas(Modifier.matchParentSize()) {
                    val stroke = 2.dp.toPx()
                    val radius = 40.dp.toPx() + stroke / 2f
                    drawCircle(
                        color = trackColor,
                        radius = radius,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    val progress = ringProgress.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        drawArc(
                            brush = arcBrush,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.fill.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    ContactAvatar(
                        avatarColor = avatarColor,
                        modifier = Modifier.size(80.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
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
            Text(
                name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.primary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = vectorResource(Res.drawable.ic_dropdown),
                contentDescription = if (isExpanded) "Collapse relationship info" else "Expand relationship info",
                tint = NekkoTheme.colors.gray.primary,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
        Spacer(Modifier.height(4.dp))

        ContactCadenceRow(frequencyLabel = frequencyLabel, reminderTime = reminderTime)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NekkoActionButton(
                text = "",
                leadingIcon = vectorResource(Res.drawable.ic_notification),
                onClick = onNotificationClick,
            )
            Spacer(Modifier.width(8.dp))

            NekkoButton(
                "Check-In",
                onClick = onCheckInClick,
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    .wrapContentHeight().wrapContentHeight(),
            )
        }
    }
}

@Composable
private fun CheckInDreamBubble(
    days: Int,
    modifier: Modifier = Modifier,
) {
    val bubbleShape = RoundedCornerShape(22.dp)
    val bubbleColor = NekkoTheme.colors.background.b0
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Main bubble
        Box(
            modifier = Modifier
                .shadow(0.dp, bubbleShape)
                .background(bubbleColor, bubbleShape)
                .widthIn(max = 180.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$days days to next\ncheck-in",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        // Two dream-bubble dots: larger near the bubble, smaller near the avatar
        Box(
            modifier = Modifier
                .padding(top = 0.dp)
                .size(width = 34.dp, height = 20.dp),
        ) {
            // Larger dot (attached under the bubble)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (-4).dp, y = (-5).dp)
                    .size(11.dp)
                    .shadow(0.dp, CircleShape)
                    .background(bubbleColor, CircleShape),
            )
            // Smaller dot (closer to the profile picture)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = (5).dp, y = (-10).dp)
                    .size(7.dp)
                    .shadow(0.dp, CircleShape)
                    .background(bubbleColor, CircleShape),
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
            tint = NekkoTheme.colors.gray.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "$frequencyLabel • $reminderTime",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
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
                avatarColor = "#007AFF",
                frequencyLabel = "Bi-weekly",
                reminderTime = "7:30AM",
                isExpanded = true,
                daysUntilNextCheckIn = 12,
                ringProgress = 0.6f,
                onNameClick = {},
                onNotificationClick = {},
                onCheckInClick = {},
            )
        }
    }
}
