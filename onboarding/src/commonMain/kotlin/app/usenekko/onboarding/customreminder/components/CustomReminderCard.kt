package app.usenekko.onboarding.customreminder.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.onboarding.customreminder.ReminderItem
import app.usenekko.theme.NekkoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_calender
import nekko.onboarding.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.vectorResource
import nekko.onboarding.generated.resources.action_remove
import nekko.onboarding.generated.resources.reminder_recurrence_cd
import nekko.onboarding.generated.resources.reminder_recurrence_label
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CustomReminderCard(
    reminder: ReminderItem,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showSwipeHint: Boolean = false,
    onSwipeHintShown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val deleteWidth = 80.dp
    val swipeGap = 8.dp
    val swipeDistancePx = with(density) { (deleteWidth + swipeGap).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // One-time affordance: nudge the card a little to the left so first-time
    // users discover the hidden delete action, then spring back into place.
    LaunchedEffect(showSwipeHint) {
        if (showSwipeHint) {
            delay(600.milliseconds)
            offset.animateTo(-swipeDistancePx * 0.18f, tween(durationMillis = 350))
            delay(150.milliseconds)
            offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
            onSwipeHintShown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clipToBounds(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        // Hidden delete action revealed by dragging the card to the left.
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .width(deleteWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background( Color(0xFF450A0A))
                    .clickable {
                        scope.launch { offset.animateTo(0f) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_trashbin),
                        contentDescription = stringResource(Res.string.action_remove),
                        tint = NekkoTheme.colors.red.active,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(Res.string.action_remove),
                        color = NekkoTheme.colors.red.hover,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .clip(RoundedCornerShape(24.dp))
                .background(NekkoTheme.colors.background.b1)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .then(
                    if (onDelete != null) {
                        Modifier.pointerInput(reminder.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offset.snapTo(
                                            (offset.value + dragAmount).coerceIn(
                                                -swipeDistancePx,
                                                0f
                                            ),
                                        )
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
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = reminder.title,
                        style = NekkoTheme.typography.heading4Semibold,
                        color = NekkoTheme.colors.text.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_calender),
                            contentDescription = stringResource(Res.string.reminder_recurrence_cd),
                            tint = NekkoTheme.colors.text.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.reminder_recurrence_label),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = NekkoTheme.colors.text.tertiary
                        )
                        Text(
                            text = reminder.recurrence,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981) // Green color based on design
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NekkoTheme.colors.fill.tertiary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = reminder.date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.primary
                    )
                }
            }
        }
    }
}
