package app.usenekko.home.presentation.badges

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.home.domain.Badge
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.bg_collect
import nekko.home.generated.resources.ic_close
import nekko.home.generated.resources.ic_soil
import org.jetbrains.compose.resources.painterResource
import nekko.home.generated.resources.action_skip
import nekko.home.generated.resources.badges_tap_reveal
import nekko.home.generated.resources.cd_close
import nekko.home.generated.resources.cd_soil
import nekko.home.generated.resources.plant_unlocked
import org.jetbrains.compose.resources.stringResource

enum class PlantRewardStage {
    Soil,
    Unlocked,
}

fun plantRewardStage(initiallyRevealed: Boolean): PlantRewardStage {
    return if (initiallyRevealed) PlantRewardStage.Unlocked else PlantRewardStage.Soil
}

fun PlantRewardStage.reveal(): PlantRewardStage = PlantRewardStage.Unlocked

@Composable
fun PlantRewardOverlay(
    badge: Badge,
    onCollect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyRevealed: Boolean = false,
) {
    var stage by remember(initiallyRevealed) {
        mutableStateOf(plantRewardStage(initiallyRevealed))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = { plantRewardTransition() },
            label = "plant reward reveal",
        ) { rewardStage ->
            when (rewardStage) {
                PlantRewardStage.Soil -> SoilRewardCard(
                    onReveal = { stage = stage.reveal() },
                    onDismiss = onDismiss,
                )

                PlantRewardStage.Unlocked -> PlantUnlockedBadgeOverlay(
                    badge = badge,
                    onCollect = onCollect,
                    onDismiss = onDismiss,
                    initiallyRevealed = true,
                )
            }
        }
    }
}

private fun plantRewardTransition(): ContentTransform {
    return (
        fadeIn(animationSpec = tween(320)) +
            scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
            )
        ) togetherWith (
        fadeOut(animationSpec = tween(150)) +
            scaleOut(targetScale = 1.04f, animationSpec = tween(150))
        )
}

@Composable
private fun SoilRewardCard(
    onReveal: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .widthIn(max = 377.dp)
            .heightIn(min = 560.dp, max = 574.dp)
            .clip(RoundedCornerShape(44.dp))
            .background(NekkoTheme.colors.background.b0)
            .clickable(onClick = onReveal),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.bg_collect),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.cd_close),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onDismiss),
                )

                Text(
                    text = stringResource(Res.string.action_skip),
                    color = NekkoTheme.colors.text.secondary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.plant_unlocked),
                color = NekkoTheme.colors.text.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )



            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_soil),
                    contentDescription = stringResource(Res.string.cd_soil),
                    modifier = Modifier.size(151.dp, 58.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(NekkoTheme.colors.background.b0)
                    .clickable(onClick = onReveal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.badges_tap_reveal),
                    color = NekkoTheme.colors.background.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewPlantRewardOverlay() {
    NekkoTheme {
        PlantRewardOverlay(
            badge = Badge(
                id = "preview",
                name = "Sunflower",
                description = "A bright reminder of the consistency you are building.",
                threshold = 150,
            ),
            onCollect = {},
            onDismiss = {},
        )
    }
}