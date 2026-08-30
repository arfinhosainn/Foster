package app.usefoster.designsystem.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.usefoster.theme.FosterTheme
import foster.shared.generated.resources.Res
import foster.shared.generated.resources.ic_acquaintance
import foster.shared.generated.resources.ic_dropdown
import foster.shared.generated.resources.ic_family
import foster.shared.generated.resources.ic_friends
import foster.shared.generated.resources.ic_group
import foster.shared.generated.resources.ic_person
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

private const val MenuWidth = 200
private const val RowHeight = 56
private const val MenuCorner = 24
private const val TriggerCorner = 28

data class AudienceOption(
    val label: String,
    val icon: DrawableResource,
)

@Composable
fun FosterDropDownButton(
    options: List<AudienceOption>,
    selected: AudienceOption,
    onSelect: (AudienceOption) -> Unit,
    modifier: Modifier = Modifier,
    chevron: DrawableResource,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuMounted by remember { mutableStateOf(false) }
    val menuProgress = remember { Animatable(0f) }

    LaunchedEffect(expanded) {
        if (expanded) {
            menuMounted = true
            menuProgress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 350f))
        } else {
            menuProgress.animateTo(0f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
            menuMounted = false
        }
    }

    Box(modifier) {
        AudienceTrigger(
            label = selected.label,
            chevron = chevron,
            expanded = expanded,
            onClick = { expanded = !expanded },
        )

        if (menuMounted) {
            Popup(
                popupPositionProvider = BelowAnchorProvider(gapPx = 12),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AudienceMenu(
                    options = options,
                    selected = selected,
                    onSelect = {
                        onSelect(it)
                        expanded = false
                    },
                    modifier = Modifier.graphicsLayer {
                        val progress = menuProgress.value
                        alpha = progress
                        val scale = 0.9f + 0.1f * progress
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        translationY = (1f - progress) * -10.dp.toPx()
                    },
                )
            }
        }
    }
}

@Composable
private fun AudienceTrigger(
    label: String,
    chevron: DrawableResource,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val squeeze by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "triggerSqueeze",
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "chevronRotation",
    )

    Row(
        Modifier
            .scale(squeeze)
            .shadow(2.dp, CircleShape, ambientColor = FosterTheme.colors.fill.tertiary)
            .background(FosterTheme.colors.background.b1, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.DropdownList,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(start = 24.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = FosterTheme.colors.text.secondary,
            style = FosterTheme.typography.heading4.copy(fontWeight = FontWeight.SemiBold),
        )
        Icon(
            imageVector = vectorResource(chevron),
            contentDescription = null,
            tint = FosterTheme.colors.text.secondary,
            modifier = Modifier.rotate(chevronRotation),
        )
    }
}

@Composable
private fun AudienceMenu(
    options: List<AudienceOption>,
    selected: AudienceOption,
    onSelect: (AudienceOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(MenuCorner.dp)

    Column(
        modifier
            .width(MenuWidth.dp)
            .shadow(2.dp, shape)
            .background(FosterTheme.colors.background.b1, shape)
            .clip(shape),
    ) {
        options.forEachIndexed { index, option ->
            AudienceRow(
                option = option,
                selected = option == selected,
                onClick = { onSelect(option) },
            )
            if (index != options.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(FosterTheme.colors.fill.tertiary),
                )
            }
        }
    }
}

@Composable
private fun AudienceRow(
    option: AudienceOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val contentColor by animateColorAsState(
        targetValue = if (selected) FosterTheme.colors.text.primary
        else FosterTheme.colors.gray.secondary,
        animationSpec = tween(180),
        label = "rowContent",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight.dp)
            .background(
                if (pressed) FosterTheme.colors.fill.tertiary else Color.Transparent
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(option.icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = option.label,
            color = contentColor,
            style = FosterTheme.typography.body,
        )
    }
}

private class BelowAnchorProvider(private val gapPx: Int) : PopupPositionProvider {


    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val y = anchorBounds.bottom + gapPx
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

@PreviewLightDark
@Composable
fun PreviewDropDownMenu(){
    FosterTheme{
        val options = listOf(
            AudienceOption("Everyone", Res.drawable.ic_group),
            AudienceOption("Family", Res.drawable.ic_family),
            AudienceOption("Friends", Res.drawable.ic_friends),
            AudienceOption("Acquaintance", Res.drawable.ic_acquaintance),
            AudienceOption("Others", Res.drawable.ic_person),
        )
        var selected by remember { mutableStateOf(options.first()) }

        FosterDropDownButton(
            options = options,
            selected = selected,
            onSelect = { selected = it },
            chevron = Res.drawable.ic_dropdown,
        )
    }
}