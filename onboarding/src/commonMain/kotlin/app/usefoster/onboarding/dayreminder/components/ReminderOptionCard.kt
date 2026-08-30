package app.usefoster.onboarding.dayreminder.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.ic_circlecheck
import org.jetbrains.compose.resources.vectorResource
import foster.onboarding.generated.resources.cd_selected
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReminderOptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(FosterTheme.colors.fill.tertiary)
            .clickable(onClick = onClick)
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_circlecheck),
                    contentDescription = stringResource(Res.string.cd_selected),
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = FosterTheme.typography.heading4Semibold,
                color = FosterTheme.colors.text.primary,
            )
        }
    }
}
