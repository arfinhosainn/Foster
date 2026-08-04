package app.usenekko.home.presentation.brainstorm.components

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
import app.usenekko.theme.NekkoTheme

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
            .background(NekkoTheme.colors.green.active.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = NekkoTheme.typography.bodyMedium,
            color = NekkoTheme.colors.text.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "OK",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NekkoTheme.colors.green.active,
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
        style = NekkoTheme.typography.bodyMedium,
        color = Color(0xFFFF4B4B),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF4B4B).copy(alpha = 0.08f))
            .padding(16.dp),
    )
}
