package app.usefoster.designsystem.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usefoster.theme.FosterTheme
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquid
import foster.shared.generated.resources.Res
import foster.shared.generated.resources.ic_contact
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource


@Composable
fun GlassIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    liquidState: LiquidState,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 16.dp,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squeeze by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "squeeze",
    )
    val isDark = isSystemInDarkTheme()

// Tint MUST be translucent, or refraction is discarded.
    val glassTint = if (isDark) {
        FosterTheme.colors.background.b3.copy(alpha = 0.28f)
    } else {
        // Additive rim light can't darken, so light mode needs the glass
        // to read slightly DARKER than the content behind it.
        Color.Black.copy(alpha = 0.06f)
    }
    val rimWidth = if (isDark) 0.5f else 0.15f
    val strokeColor = if (isDark) Color.White.copy(alpha = 0.18f)
    else Color.Black.copy(alpha = 0.10f)


    Box(
        modifier
            .size(size)
            .scale(squeeze)
            .background(Color.Transparent, CircleShape)
            .selectable(
                selected = false,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .liquid(liquidState) {
                frost = 5.dp
                shape = CircleShape
                refraction = 0.35f
                curve = 0.30f
                edge = 0.5f
                tint = glassTint
                saturation = 1.15f
                dispersion = 0f
            }
            .border(1.dp, strokeColor, CircleShape),

        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewGlassIconButton() = FosterTheme {

    Surface {
        rememberLiquidState().let { liquidState ->
            GlassIconButton(
                icon = Res.drawable.ic_contact,
                contentDescription = "Invite someone",
                onClick = {},
                liquidState = liquidState,
            )
        }

    }
}