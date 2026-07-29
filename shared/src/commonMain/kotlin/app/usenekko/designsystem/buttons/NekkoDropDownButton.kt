package app.usenekko.designsystem.buttons

import androidx.compose.animation.animateColorAsState
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
import app.usenekko.theme.NekkoTheme
import nekko.shared.generated.resources.Res
import nekko.shared.generated.resources.ic_acquaintance
import nekko.shared.generated.resources.ic_dropdown
import nekko.shared.generated.resources.ic_family
import nekko.shared.generated.resources.ic_friends
import nekko.shared.generated.resources.ic_group
import nekko.shared.generated.resources.ic_person
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

private const val MenuWidth = 240
private const val RowHeight = 56
private const val MenuCorner = 24
private const val TriggerCorner = 28

data class AudienceOption(
    val label: String,
    val icon: DrawableResource,
)

@Composable
fun NekkoDropDownButton(
    options: List<AudienceOption>,
    selected: AudienceOption,
    onSelect: (AudienceOption) -> Unit,
    modifier: Modifier = Modifier,
    chevron: DrawableResource,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        AudienceTrigger(
            label = selected.label,
            chevron = chevron,
            expanded = expanded,
            onClick = { expanded = !expanded },
        )

        if (expanded) {
            Popup(
                popupPositionProvider = BelowAnchorProvider(gapPx = 12),
                onDismissRequest = { expanded = false },
            ) {
                AudienceMenu(
                    options = options,
                    selected = selected,
                    onSelect = {
                        onSelect(it)
                        expanded = false
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
            .shadow(10.dp, CircleShape, ambientColor = NekkoTheme.colors.fill.tertiary)
            .background(NekkoTheme.colors.background.b1, CircleShape)
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
            color = NekkoTheme.colors.text.secondary,
            style = NekkoTheme.typography.heading4.copy(fontWeight = FontWeight.SemiBold),
        )
        Icon(
            imageVector = vectorResource(chevron),
            contentDescription = null,
            tint = NekkoTheme.colors.text.secondary,
            modifier = Modifier.rotate(chevronRotation),
        )
    }
}

@Composable
private fun AudienceMenu(
    options: List<AudienceOption>,
    selected: AudienceOption,
    onSelect: (AudienceOption) -> Unit,
) {
    val shape = RoundedCornerShape(MenuCorner.dp)

    Column(
        Modifier
            .width(MenuWidth.dp)
            .shadow(20.dp, shape)
            .background(NekkoTheme.colors.background.b1, shape)
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
                        .background(NekkoTheme.colors.fill.tertiary),
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
        targetValue = if (selected) NekkoTheme.colors.text.primary
        else NekkoTheme.colors.gray.secondary,
        animationSpec = tween(180),
        label = "rowContent",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight.dp)
            .background(
                if (pressed) NekkoTheme.colors.fill.tertiary else Color.Transparent
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
            style = NekkoTheme.typography.body,
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
    NekkoTheme{
        val options = listOf(
            AudienceOption("Everyone", Res.drawable.ic_group),
            AudienceOption("Family", Res.drawable.ic_family),
            AudienceOption("Friends", Res.drawable.ic_friends),
            AudienceOption("Acquaintance", Res.drawable.ic_acquaintance),
            AudienceOption("Others", Res.drawable.ic_person),
        )
        var selected by remember { mutableStateOf(options.first()) }

        NekkoDropDownButton(
            options = options,
            selected = selected,
            onSelect = { selected = it },
            chevron = Res.drawable.ic_dropdown,
        )
    }
}