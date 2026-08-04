package app.usenekko.home.presentation.badges

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.home.domain.Badge
import app.usenekko.home.domain.BadgeSlot
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_greenflower
import nekko.home.generated.resources.ic_sprout
import nekko.home.generated.resources.ic_sunflower
import nekko.home.generated.resources.ic_tree
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Maps a badge's unlock threshold to its plant artwork.
 *
 * Tier 1 (Seedling) uses `ic_sprout`, Tier 2 (Wild Flower) uses `ic_greenflower`,
 * Tier 3 (Grove Keeper) uses `ic_sunflower`, Tier 4 (Towering Oak) uses
 * `ic_tree`. `ic_sprout` and `ic_tree` are NEW placeholder vectors; the flower
 * icons are the existing Compose drawables.
 */
fun badgeIcon(threshold: Int): DrawableResource = when (threshold) {
    1 -> Res.drawable.ic_sprout
    15 -> Res.drawable.ic_greenflower
    50 -> Res.drawable.ic_sunflower
    else -> Res.drawable.ic_tree
}

@Composable
fun BadgeRow(
    badges: List<BadgeSlot>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Badges",
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
                .size(64.dp)
                .background(NekkoTheme.colors.fill.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (slot.unlocked) {
                Image(
                    painter = painterResource(badgeIcon(slot.badge.threshold)),
                    contentDescription = slot.badge.name,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Image(
                    painter = painterResource(badgeIcon(slot.badge.threshold)),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    alpha = 0.45f,
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

/**
 * Full-screen two-step reveal overlay for a freshly unlocked badge.
 * Step 1: "TAP TO REVEAL" over a bare dirt mound. Step 2: the plant grows out
 * of the same mound, showing the badge name + description and a Collect button.
 */
@Composable
fun PlantUnlockedBadgeOverlay(
    badge: Badge,
    onCollect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE613161B))
            .clickable(enabled = !revealed) { revealed = true },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(44.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                color = NekkoTheme.colors.text.primary,
                fontSize = 22.sp,
            )
        }
        Text(
            text = "Skip",
            color = NekkoTheme.colors.text.tertiary,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clickable(onClick = onDismiss),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (revealed) badge.name else "Plant Unlocked",
                color = NekkoTheme.colors.text.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            if (revealed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = badge.description,
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(40.dp))

            DirtMound(plant = if (revealed) badgeIcon(badge.threshold) else null)

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (revealed) "PLANT UNLOCKED" else "TAP TO REVEAL",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
            )

            if (revealed) {
                Spacer(Modifier.height(40.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(NekkoTheme.colors.background.b1, RoundedCornerShape(28.dp))
                        .clickable(onClick = onCollect),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Collect Plant",
                        color = NekkoTheme.colors.text.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirtMound(
    plant: DrawableResource?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(220.dp, 150.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (plant != null) {
            Image(
                painter = painterResource(plant),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(96.dp)
                    .padding(bottom = 42.dp),
            )
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(160.dp, 58.dp),
        ) {
            drawOval(
                color = Color(0xFFB98A4E),
                topLeft = Offset(size.width * 0.06f, 0f),
                size = Size(size.width * 0.88f, size.height),
            )
            drawOval(
                color = Color(0xFFD9B179),
                topLeft = Offset(size.width * 0.16f, size.height * 0.12f),
                size = Size(size.width * 0.68f, size.height * 0.7f),
            )
            // Small leaf detail resting on the mound.
            drawOval(
                color = Color(0xFF4CC05F),
                topLeft = Offset(size.width * 0.30f, 2.dp.toPx()),
                size = Size(size.width * 0.14f, size.height * 0.28f),
            )
        }
    }
}
