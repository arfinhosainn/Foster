package app.usenekko.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.usenekko.theme.NekkoTheme

@Composable
fun StepIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(NekkoTheme.colors.fill.secondary)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isCurrent = index == currentStep
            val color by animateColorAsState(
                targetValue = if (isCurrent) {
                    NekkoTheme.colors.background.onBackground
                } else {
                    NekkoTheme.colors.gray.quaternary
                },
                label = "step_color_$index",
            )
            val width = if (isCurrent) 18.dp else 6.dp

            Box(
                modifier = Modifier
                    .width(width)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}
