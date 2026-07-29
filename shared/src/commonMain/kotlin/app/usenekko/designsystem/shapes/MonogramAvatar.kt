package app.usenekko.designsystem.shapes


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme

@Composable
fun MonogramAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = NekkoTheme.colors.gray.secondary,
    contentColor: Color = NekkoTheme.colors.text.primary,
) {
    val initials = remember(name) { name.toInitials() }

    Box(
        modifier
            .size(size)
            .background(containerColor, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = NekkoTheme.typography.heading3Bold.copy(
                fontSize = (size.value * 0.36f).sp,   // scales with the circle
                lineHeight = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
            ),
        )
    }
}

private fun String.toInitials(): String = trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifEmpty { "?" }


