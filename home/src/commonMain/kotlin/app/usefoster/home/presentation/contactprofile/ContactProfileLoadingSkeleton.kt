package app.usefoster.home.presentation.contactprofile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.usefoster.theme.FosterTheme

@Composable
fun ContactProfileLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "contactProfileShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "contactProfileShimmerPosition",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            FosterTheme.colors.fill.quaternary,
            FosterTheme.colors.fill.secondary,
            FosterTheme.colors.fill.quaternary,
        ),
        start = Offset(shimmerPosition * 500f, 0f),
        end = Offset((shimmerPosition + 1f) * 500f, 500f),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(shimmerBrush, CircleShape),
        )
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBlock(
            brush = shimmerBrush,
            modifier = Modifier
                .width(148.dp)
                .height(22.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonBlock(
            brush = shimmerBrush,
            modifier = Modifier
                .width(124.dp)
                .height(16.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SkeletonBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .size(width = 48.dp, height = 44.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .width(132.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        SkeletonBlock(
            brush = shimmerBrush,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(24.dp)),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SkeletonBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .width(92.dp)
                    .height(22.dp),
            )
            SkeletonBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .width(66.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        repeat(2) {
            SkeletonBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(brush),
    )
}