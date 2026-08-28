package app.usenekko.designsystem.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.sideShine
import app.usenekko.theme.NekkoTheme

@Composable
fun NekkoActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    containerColor: Color = NekkoTheme.colors.background.b1,
    contentColor: Color = NekkoTheme.colors.text.primary,
    iconTint: Color = NekkoTheme.colors.text.primary,
    iconSize: Dp = 20.dp,
    minWidth: Dp = 56.dp, // floor so icon-only stays a pill, never a perfect circle
    textStyle: TextStyle = NekkoTheme.typography.heading4Semibold,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = if (!text.isNullOrBlank()) 24.dp else 18.dp,
        vertical = 14.dp,
    ),
) {
    val shape = RoundedCornerShape(50)
    val hasText = !text.isNullOrBlank()

    Box(
        modifier
            .defaultMinSize(minWidth = minWidth)
            .nekkoActionShell(shape, containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var contentAdded = false

            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
                contentAdded = true
            }

            if (hasText) {
                if (contentAdded) Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = textStyle,
                    color = contentColor,
                )
                contentAdded = true
            }

            if (trailingIcon != null) {
                if (contentAdded) Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

/**
 * The Nekko action-button surface: zero-elevation shadow, clipped fill, and
 * the edge-lit side shine. Shared so composite controls built on the same
 * look (segmented toggles, shells around live content) stay in sync with
 * [NekkoActionButton].
 */
fun Modifier.nekkoActionShell(shape: Shape, containerColor: Color): Modifier =
    this
        .dropShadow(shape = shape, shadow = Shadow(0.dp))
        .clip(shape)
        .background(containerColor)
        .sideShine(shape)