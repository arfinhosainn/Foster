package app.usefoster.home.presentation.brainstorm.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.home.presentation.brainstorm.BrainstormTab
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_point
import foster.home.generated.resources.brainstorm_send_message_cd
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrainstormTopBar(
    modifier: Modifier = Modifier,
) {
    Text(
        "Brainstorm",
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        color = FosterTheme.colors.text.primary,
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

@Composable
fun BrainstormTabs(
    selected: BrainstormTab,
    onSelect: (BrainstormTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(BrainstormTab.CurrentOutput to "Current Output", BrainstormTab.History to "History")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        val isWideLayout = maxWidth >= 600.dp
        val controlHeight = if (isWideLayout) 86.dp else 46.dp
        val controlPadding = if (isWideLayout) 4.dp else 3.dp
        val controlShape = RoundedCornerShape(if (isWideLayout) 30.dp else 20.dp)
        val segmentShape = RoundedCornerShape(if (isWideLayout) 28.dp else 18.dp)
        val labelStyle = if (isWideLayout) {
            FosterTheme.typography.heading1.copy(fontSize = 36.sp, lineHeight = 43.sp)
        } else {
            FosterTheme.typography.bodyMedium
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(controlHeight)
                .clip(controlShape)
                .background(FosterTheme.colors.fill.quaternary)
                .padding(controlPadding),
        ) {
            options.forEach { (tab, label) ->
                val isSelected = tab == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = segmentShape,
                    color = if (isSelected) {
                        FosterTheme.colors.background.b2
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    border = if (isSelected) {
                        BorderStroke(0.dp, FosterTheme.colors.stroke.secondary)
                    } else {
                        null
                    },
                    onClick = { onSelect(tab) },
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(
                            text = label,
                            style = labelStyle,
                            color = if (isSelected) {
                                FosterTheme.colors.text.primary
                            } else {
                                FosterTheme.colors.text.tertiary
                            },
                            fontWeight = if (isWideLayout) FontWeight.Normal else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopicCard(
    topic: BrainstormTopic,
    onSendMessage: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val canSendMessage = onSendMessage != null && !topic.description.isNullOrBlank()

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(FosterTheme.colors.fill.tertiary)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    imageVector = vectorResource(Res.drawable.ic_point),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = topic.title,
                    style = FosterTheme.typography.heading4,
                    color = FosterTheme.colors.text.primary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = topic.description.orEmpty(),
                style = FosterTheme.typography.heading4,
                fontWeight = FontWeight.Normal,
                color = FosterTheme.colors.text.secondary,
                // Keep clear of the send icon pinned to the bottom-right corner.
                modifier = Modifier.padding(end = 36.dp, bottom = 4.dp),
            )
        }

        if (canSendMessage) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FosterTheme.colors.fill.secondary)
                    .clickable(onClick = onSendMessage),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.brainstorm_send_message_cd),
                    tint = FosterTheme.colors.text.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun ShimmerTopicCard(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "brainstormShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "brainstormShimmerPosition",
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
            .clip(RoundedCornerShape(24.dp))
            .background(shimmerBrush)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FosterTheme.colors.text.tertiary.copy(alpha = 0.18f)),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(FosterTheme.colors.text.tertiary.copy(alpha = 0.14f)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(FosterTheme.colors.text.tertiary.copy(alpha = 0.14f)),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}
