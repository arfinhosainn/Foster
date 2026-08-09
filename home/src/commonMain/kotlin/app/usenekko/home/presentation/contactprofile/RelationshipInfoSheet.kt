package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.usenekko.home.presentation.components.ContactAvatar
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.grass_1
import nekko.home.generated.resources.grass_2
import nekko.home.generated.resources.grass_3
import nekko.home.generated.resources.grass_4
import nekko.home.generated.resources.grass_5
import nekko.home.generated.resources.ic_forwardarrow
import nekko.home.generated.resources.ic_sprout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipInfoSheet(
    contactName: String,
    avatarColor: String?,
    userSelectedAvatarId: String?,
    checkInCount: Int,
    nextCheckInDate: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_forwardarrow),
                            contentDescription = "Close check-in details",
                            tint = NekkoTheme.colors.text.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
//                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = contactName,
                        style = NekkoTheme.typography.heading2Bold,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.primary,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Next check-in: ${formatLongCheckInDate(nextCheckInDate)}",
                    style = NekkoTheme.typography.heading4,
                    fontWeight = FontWeight.SemiBold,
                    color = NekkoTheme.colors.text.tertiary,
                    modifier = Modifier.padding(horizontal = 35.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                CheckInStatsCard(
                    avatarColor = avatarColor,
                    userSelectedAvatarId = userSelectedAvatarId,
                    checkInCount = checkInCount,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Updated just now",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = NekkoTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                DashedDivider(modifier = Modifier.padding(horizontal = 24.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            GrassProgress(checkInCount = checkInCount)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CheckInStatsCard(
    avatarColor: String?,
    userSelectedAvatarId: String?,
    checkInCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(95.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.tertiary),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckInCountSummary(
            checkInCount = checkInCount,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(NekkoTheme.colors.fill.tertiary),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(52.dp),
            ) {
                RelationshipAvatar(
                    avatarColor = avatarColor,
                    modifier = Modifier.zIndex(0f),
                )
                RelationshipAvatar(
                    selectedAvatarId = userSelectedAvatarId ?: "0",
                    modifier = Modifier
                        .offset(x = 28.dp)
                        .zIndex(1f),
                )
            }
        }
    }
}

@Composable
private fun RelationshipAvatar(
    avatarColor: String? = null,
    selectedAvatarId: String? = null,
    modifier: Modifier = Modifier,
) {
    ContactAvatar(
        avatarColor = avatarColor,
        selectedAvatarId = selectedAvatarId,
        modifier = modifier
            .size(46.dp)
            .border(
                width = 1.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        NekkoTheme.colors.green.active,
                        NekkoTheme.colors.yellow.active,
                        NekkoTheme.colors.green.active,
                    ),
                ),
                shape = CircleShape,
            )
            .padding(1.dp),
    )
}

@Composable
private fun CheckInCountSummary(
    checkInCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = NekkoTheme.colors.background.b2,
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_sprout),
                    contentDescription = "Check-in growth",
                    modifier = Modifier.size(width = 16.dp, height = 21.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = checkInCountText(checkInCount),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = NekkoTheme.colors.text.primary,
            )
            Text(
                text = "Check-ins",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.tertiary,
            )
        }
    }
}

fun checkInCountText(checkInCount: Int): String = checkInCount.toString()

@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(32) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NekkoTheme.colors.fill.tertiary),
            )
        }
    }
}

@Composable
private fun GrassProgress(
    checkInCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painter = painterResource(grassResourceForCheckInCount(checkInCount)),
            contentDescription = "Relationship growth",
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }
}

fun grassStageForCheckInCount(checkInCount: Int): Int = when {
    checkInCount <= 0 -> 1
    checkInCount <= 2 -> 2
    checkInCount <= 5 -> 3
    checkInCount <= 9 -> 4
    else -> 5
}

private fun grassResourceForCheckInCount(checkInCount: Int): DrawableResource = when (
    grassStageForCheckInCount(checkInCount)
) {
    1 -> Res.drawable.grass_1
    2 -> Res.drawable.grass_2
    3 -> Res.drawable.grass_3
    4 -> Res.drawable.grass_4
    else -> Res.drawable.grass_5
}

private fun formatLongCheckInDate(date: String?): String {
    if (date == null) return "No upcoming"
    return runCatching {
        val parsed = kotlinx.datetime.LocalDate.parse(date)
        val month = parsed.month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
        val day = parsed.day
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        "$day$suffix $month, ${parsed.year}"
    }.getOrDefault("No upcoming")
}
