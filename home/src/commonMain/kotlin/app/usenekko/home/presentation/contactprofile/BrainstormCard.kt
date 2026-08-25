package app.usenekko.home.presentation.contactprofile

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
import app.usenekko.designsystem.leftEdgeShine
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_brainstrom
import nekko.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.painterResource
import nekko.home.generated.resources.brainstorm_action
import nekko.home.generated.resources.brainstorm_ai_hint
import nekko.home.generated.resources.brainstorm_topics
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

            .background(NekkoTheme.colors.fill.quaternary)
            .leftEdgeShine(BRAINSTORM_CARD_SHAPE) // ← edge-lit shine, left side only
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(Res.drawable.img_gradientss),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxHeight(0.9f)
                .aspectRatio(1199f / 540f),
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
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.brainstorm_ai_hint),
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(NekkoTheme.colors.background.b1)
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
                    color = NekkoTheme.colors.text.primary,
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
    NekkoTheme {
        Surface(
            color = NekkoTheme.colors.background.b0,
        ) {
            BrainstormCard(
                onClick = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}