package app.usefoster.home.presentation.brainstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.action_ok
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NoticeBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FosterTheme.colors.green.active.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = FosterTheme.typography.bodyMedium,
            color = FosterTheme.colors.text.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.action_ok),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FosterTheme.colors.green.active,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun ErrorBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = FosterTheme.typography.bodyMedium,
        color = Color(0xFFFF4B4B),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF4B4B).copy(alpha = 0.08f))
            .padding(16.dp),
    )
}
