package app.usenekko.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_fire
import nekko.home.generated.resources.ic_globe
import nekko.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun StatusSummaryCard(
    outstandingCount: Int,
    upToDateCount: Int,
    outstandingBgResource: DrawableResource?,
    upToDateBgResource: DrawableResource?,
    gradientOrbResource: DrawableResource?,   // ← New parameter
    modifier: Modifier = Modifier,
    onOutstandingClick: () -> Unit = {},
    onUpToDateClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = NekkoTheme.colors.background.b1,
        shadowElevation = 0.5.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Gradient Orb at the bottom center (behind everything)
            if (gradientOrbResource != null) {
                Image(
                    painter = painterResource(gradientOrbResource),
                    contentDescription = null,
                    modifier = Modifier
                        .blur(radius = 30.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusItem(
                    count = outstandingCount,
                    label = "Outstanding",
                    bgResource = outstandingBgResource,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOutstandingClick)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(0.8.dp)
                        .height(80.dp)
                        .background(NekkoTheme.colors.fill.quaternary)
                )

                StatusItem(
                    count = upToDateCount,
                    label = "Up to date",
                    bgResource = upToDateBgResource,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onUpToDateClick)
                )
            }
        }
    }
}
@Composable
private fun StatusItem(
    count: Int,
    label: String,
    bgResource: DrawableResource?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(92.dp),           // Slightly bigger container
            contentAlignment = Alignment.Center
        ) {
            // Glowing Background
            if (bgResource != null) {
                Image(
                    painter = painterResource(bgResource),
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .blur(radius = 10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            } else {
                // Fallback
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFF2F2F7), RoundedCornerShape(20.dp))
                )
            }

            // Count Number
            Text(
                text = count.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NekkoTheme.colors.text.primary,
                modifier = Modifier.offset(y = (20).dp)   // Fine-tune vertical position
            )
        }

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8E8E93),
            letterSpacing = 0.2.sp
        )
    }
}

@PreviewLightDark
@Composable
fun PreviewStatusSummaryCard() {
    NekkoTheme {
        StatusSummaryCard(
            outstandingCount = 0,
            upToDateCount = 0,
            outstandingBgResource = Res.drawable.ic_globe,
            upToDateBgResource = Res.drawable.ic_fire,
            gradientOrbResource = Res.drawable.img_gradientss
        )
    }
}