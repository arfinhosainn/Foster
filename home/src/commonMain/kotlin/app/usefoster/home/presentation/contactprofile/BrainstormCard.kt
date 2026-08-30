package app.usefoster.home.presentation.contactprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.designsystem.leftEdgeShine
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_brainstrom
import foster.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.painterResource
import foster.home.generated.resources.brainstorm_action
import foster.home.generated.resources.brainstorm_ai_hint
import foster.home.generated.resources.brainstorm_topics
import org.jetbrains.compose.resources.stringResource

private val BRAINSTORM_CARD_HEIGHT = 133.dp
private val BRAINSTORM_CARD_SHAPE = RoundedCornerShape(24.dp)

@Composable
fun BrainstormCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BRAINSTORM_CARD_HEIGHT)
            .clip(BRAINSTORM_CARD_SHAPE)

            .background(FosterTheme.colors.fill.quaternary)
            .leftEdgeShine(BRAINSTORM_CARD_SHAPE, intensity = 0.4f) // ← edge-lit shine, left side only
            .clickable(onClick = onClick),
    ) {
        // Sized so it doesn't fill the whole card (Crop + no size would stretch it
        // edge-to-edge and lock it to the middle). Aligned to BottomEnd, then offset
        // further down/right so the blob's hotspot sits in the bottom-end corner;
        // the overflow is clipped by the card's shape.
        Image(
            painter = painterResource(Res.drawable.img_gradientss),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = 0.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.brainstorm_topics),
                    color = FosterTheme.colors.text.primary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.brainstorm_ai_hint),
                    color = FosterTheme.colors.text.tertiary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(FosterTheme.colors.background.b1)
                    .padding(start = 10.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_brainstrom),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(Res.string.brainstorm_action),
                    color = FosterTheme.colors.text.primary,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewBrainstormCard() {
    FosterTheme {
        Surface(
            color = FosterTheme.colors.background.b0,
        ) {
            BrainstormCard(
                onClick = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}