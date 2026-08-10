package app.usenekko.designsystem.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    iconTint: Color = NekkoTheme.colors.background.onBackground,
    iconSize: Dp = 20.dp,
    minWidth: Dp = 56.dp, // floor so icon-only stays a pill, never a perfect circle
) {
    val shape = RoundedCornerShape(50)
    val hasText = !text.isNullOrBlank()

    // Vertical padding stays fixed — height shouldn't change based on content.
    val verticalPadding = 14.dp
    // Horizontal padding stays generous even icon-only, so width > height on its own;
    // minWidth below is just a backstop for very small icons/edge cases.
    val horizontalPadding = if (hasText) 24.dp else 18.dp

    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier
            .defaultMinSize(minWidth = minWidth)
            .clip(shape)
            .dropShadow(shape = shape, shadow = Shadow(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
                    style = NekkoTheme.typography.heading4Semibold,
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