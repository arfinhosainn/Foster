package app.usenekko.home.presentation.badges

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import app.usenekko.home.domain.BadgeSlot
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.bg_collect
import nekko.home.generated.resources.blue_flower
import nekko.home.generated.resources.ic_bluelotus
import nekko.home.generated.resources.ic_brown
import nekko.home.generated.resources.ic_close
import nekko.home.generated.resources.ic_greenflower
import nekko.home.generated.resources.ic_lotus
import nekko.home.generated.resources.ic_mushroom
import nekko.home.generated.resources.ic_pinkflower
import nekko.home.generated.resources.ic_soil
import nekko.home.generated.resources.ic_sunflower
import nekko.home.generated.resources.lotus_flower
import nekko.home.generated.resources.mushroom_flower
import nekko.home.generated.resources.pink_flower
import nekko.home.generated.resources.sun_flower
import nekko.home.generated.resources.yellow_flower
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import nekko.home.generated.resources.action_skip
import nekko.home.generated.resources.badges_collect_plant
import nekko.home.generated.resources.badges_tap_reveal
import nekko.home.generated.resources.badges_title
import nekko.home.generated.resources.cd_close
import org.jetbrains.compose.resources.stringResource

/** Maps a badge to the matching flower artwork from the resource catalog. */
fun badgeIcon(badge: Badge): DrawableResource {
    return when (badgeFlowerAsset(badge)) {
        "soil" -> Res.drawable.ic_soil
        "lotus" -> Res.drawable.ic_lotus
        "mushroom" -> Res.drawable.ic_mushroom
        "pinkflower" -> Res.drawable.ic_pinkflower
        "brown" -> Res.drawable.ic_brown
        "bluelotus" -> Res.drawable.ic_bluelotus
        "sunflower" -> Res.drawable.ic_sunflower
        "greenflower" -> Res.drawable.ic_greenflower
        else -> Res.drawable.ic_greenflower
    }
}

fun badgeFlowerAsset(badge: Badge): String {
    val name = badge.name.lowercase().filter(Char::isLetter)

    return when {
        "soil" in name -> "soil"
        "blue" in name -> "bluelotus"
        "lotus" in name -> "lotus"
        "mushroom" in name -> "mushroom"
        "pink" in name || "red" in name -> "pinkflower"
        "brown" in name || "yellow" in name || "yello" in name -> "brown"
        "sunflower" in name -> "sunflower"
        "green" in name -> "greenflower"
        else -> when (badge.threshold) {
            1 -> "soil"
            15 -> "lotus"
            30 -> "sunflower"
            45 -> "brown"
            60 -> "bluelotus"
            75 -> "pinkflower"
            90 -> "greenflower"
            115 -> "mushroom"
            else -> "greenflower"
        }
    }
}

/** Maps a badge to the larger plant artwork used by the collect popup. */
fun badgeCollectArtwork(badge: Badge): DrawableResource {
    return when (badgeFlowerAsset(badge)) {
        "soil" -> Res.drawable.ic_soil
        "lotus" -> Res.drawable.lotus_flower
        "mushroom" -> Res.drawable.mushroom_flower
        "pinkflower" -> Res.drawable.pink_flower
        "brown" -> Res.drawable.yellow_flower
        "bluelotus" -> Res.drawable.blue_flower
        "sunflower" -> Res.drawable.sun_flower
        // The new artwork set has no separate green plant, so retain the
        // existing green artwork until one is provided.
        else -> Res.drawable.ic_greenflower
    }
}

@Composable
fun BadgeRow(
    badges: List<BadgeSlot>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.badges_title),
            color = NekkoTheme.colors.text.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            badges.forEach { slot ->
                BadgeSlotItem(slot, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeSlotItem(
    slot: BadgeSlot,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(NekkoTheme.colors.fill.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (slot.unlocked) {
                Image(
                    painter = painterResource(badgeIcon(slot.badge)),
                    contentDescription = slot.badge.name,
                )
            } else {
                Image(
                    painter = painterResource(badgeIcon(slot.badge)),
                    contentDescription = null,
                    alpha = 1f,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (slot.unlocked) slot.badge.name else "Locked",
            color = if (slot.unlocked) NekkoTheme.colors.text.primary else NekkoTheme.colors.text.tertiary,
            fontSize = 11.sp,
            fontWeight = if (slot.unlocked) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** Full-screen two-step reveal overlay for a freshly unlocked badge. */
@Composable
fun PlantUnlockedBadgeOverlay(
    badge: Badge,
    onCollect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyRevealed: Boolean = false,
) {
    var revealed by remember(initiallyRevealed) { mutableStateOf(initiallyRevealed) }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .widthIn(max = 377.dp)
            .heightIn(min = 560.dp, max = 574.dp)
            .clip(RoundedCornerShape(44.dp))
            .background(NekkoTheme.colors.background.b0),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.bg_collect),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (revealed) "A new plant has grown!" else "Plant unlocked",
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (revealed) badge.name else "Tap to reveal your new plant",
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(29.dp))
                if (revealed) {
                    Image(
                        painter = painterResource(badgeCollectArtwork(badge)),
                        contentDescription = badge.name,
                        modifier = Modifier.size(151.dp, 210.dp),
                    )
                    Spacer(Modifier.height(29.dp))
                    Text(
                        text = badge.description,
                        color = NekkoTheme.colors.background.onBackground,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = "✦",
                        color = Color(0xFFF2A900),
                        fontSize = 64.sp,
                        modifier = Modifier.padding(vertical = 58.dp),
                    )
                }

                Spacer(Modifier.height(29.dp))
                if (revealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(NekkoTheme.colors.background.onBackground)
                            .clickable(onClick = onCollect),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.badges_collect_plant),
                            color = NekkoTheme.colors.background.b0,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.badges_tap_reveal),
                        color = Color(0xFF718096),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
            }
        }
    }
}


@PreviewLightDark
@Composable
fun PreviewPlantUnlockedBadgeOverlay() {
    NekkoTheme {
        PlantUnlockedBadgeOverlay(
            badge = Badge(
                id = "preview",
                name = "Sunflower",
                description = "A bright reminder of the consistency you are building.",
                threshold = 150,
            ),
            onCollect = {},
            onDismiss = {},
            initiallyRevealed = true,
        )
    }
}
