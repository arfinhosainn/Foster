package app.usenekko.onboarding.customreminder.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.onboarding.customreminder.ReminderItem
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_calender
import org.jetbrains.compose.resources.vectorResource

@Composable
fun CustomReminderCard(
    reminder: ReminderItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.secondary)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = reminder.title,
                    style = NekkoTheme.typography.heading4Semibold,
                    color = NekkoTheme.colors.text.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_calender),
                        contentDescription = "Recurrence",
                        tint = NekkoTheme.colors.text.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recurrence",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.tertiary
                    )
                    Text(
                        text = reminder.recurrence,
                        fontSize = 14.sp,
                        color = Color(0xFF10B981) // Green color based on design
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NekkoTheme.colors.fill.tertiary)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = reminder.date,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.primary
                )
            }
        }
    }
}
