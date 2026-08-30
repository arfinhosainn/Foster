package app.usefoster.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.ic_checkmark
import foster.onboarding.generated.resources.ic_forward
import org.jetbrains.compose.resources.painterResource

@Composable
fun FosterStepField(
    isConfirmed: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        StepStatusIcon(isConfirmed = isConfirmed, isLoading = isLoading)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(FosterTheme.colors.fill.secondary)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun StepStatusIcon(
    isConfirmed: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = FosterTheme.colors.text.tertiary,
            trackColor = FosterTheme.colors.green.default,

        )
    } else {
        Image(
            painter = painterResource(
                if (isConfirmed) Res.drawable.ic_checkmark else Res.drawable.ic_forward
            ),
            contentDescription = null,
            modifier = modifier.size(24.dp),
        )
    }
}
