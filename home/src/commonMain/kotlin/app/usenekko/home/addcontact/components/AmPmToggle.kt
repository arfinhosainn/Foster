package app.usenekko.home.addcontact.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme

@Composable
fun AmPmToggle(
    isAm: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBg = NekkoTheme.colors.fill.primary
    val unselectedBg = NekkoTheme.colors.fill.quaternary
    val selectedTextColor = NekkoTheme.colors.text.primary
    val unselectedTextColor = NekkoTheme.colors.text.tertiary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(NekkoTheme.colors.fill.tertiary)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val amBgColor by animateColorAsState(
            targetValue = if (isAm) selectedBg else unselectedBg.copy(alpha = 0f),
            label = "amBg",
        )
        val amTextColor by animateColorAsState(
            targetValue = if (isAm) selectedTextColor else unselectedTextColor,
            label = "amText",
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(amBgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggle(true) },
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "AM",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = amTextColor,
            )
        }

        val pmBgColor by animateColorAsState(
            targetValue = if (!isAm) selectedBg else unselectedBg.copy(alpha = 0f),
            label = "pmBg",
        )
        val pmTextColor by animateColorAsState(
            targetValue = if (!isAm) selectedTextColor else unselectedTextColor,
            label = "pmText",
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pmBgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggle(false) },
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "PM",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pmTextColor,
            )
        }
    }
}