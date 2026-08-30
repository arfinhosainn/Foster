package app.usefoster.home.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usefoster.home.presentation.components.TIMELINE_COLUMNS
import app.usefoster.home.presentation.components.TIMELINE_ROWS
import app.usefoster.home.presentation.components.timelineCellSizeForWidth
import app.usefoster.home.presentation.components.timelineRowLeadingEmptyColumns
import app.usefoster.home.presentation.components.timelineRowSpacing
import app.usefoster.home.presentation.components.timelineRowSlotIndices
import app.usefoster.theme.FosterTheme

@Composable
fun HomeLoadingSkeleton(
    modifier: Modifier = Modifier,
    timelineMaxCellSize: Dp? = null,
) {
    val transition = rememberInfiniteTransition(label = "homeShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "homeShimmerPosition",
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
    ) {
        SummarySkeleton(shimmerBrush)
        Spacer(modifier = Modifier.height(32.dp))
        CheckInSkeleton(shimmerBrush, timelineMaxCellSize)
        Spacer(modifier = Modifier.height(32.dp))
        ContactListSkeleton(shimmerBrush)
    }
}

@Composable
private fun SummarySkeleton(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(FosterTheme.colors.background.b1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SummaryItemSkeleton(
            brush = brush,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(80.dp)
                .background(FosterTheme.colors.fill.quaternary),
        )
        SummaryItemSkeleton(
            brush = brush,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryItemSkeleton(
    brush: Brush,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkeletonBlock(
            brush = brush,
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(22.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBlock(
            brush = brush,
            modifier = Modifier
                .width(76.dp)
                .height(14.dp),
            shape = RoundedCornerShape(7.dp),
        )
    }
}

@Composable
private fun CheckInSkeleton(
    brush: Brush,
    maxCellSize: Dp?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SkeletonBlock(
            brush = brush,
            modifier = Modifier
                .width(84.dp)
                .height(24.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBlock(
            brush = brush,
            modifier = Modifier
                .width(204.dp)
                .height(16.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val horizontalSpacing = 8.dp
            val verticalSpacing = timelineRowSpacing()
            val cellSize = timelineCellSizeForWidth(
                maxWidth = maxWidth,
                horizontalSpacing = horizontalSpacing,
                maxCellSize = maxCellSize,
            )
            val gridWidth = cellSize * TIMELINE_COLUMNS +
                horizontalSpacing * (TIMELINE_COLUMNS - 1)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                homeLoadingTimelineRowSlotCounts().forEachIndexed { rowIndex, visibleColumns ->
                    val visualRow = TIMELINE_ROWS - 1 - rowIndex
                    val leadingEmptyColumns = timelineRowLeadingEmptyColumns(visualRow)

                    Row(
                        modifier = Modifier.size(width = gridWidth, height = cellSize),
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    ) {
                        repeat(leadingEmptyColumns) {
                            Spacer(Modifier.size(cellSize))
                        }
                        repeat(visibleColumns) {
                            SkeletonBlock(
                                brush = brush,
                                modifier = Modifier.size(cellSize),
                                shape = RoundedCornerShape(percent = 42),
                            )
                        }
                        repeat(TIMELINE_COLUMNS - leadingEmptyColumns - visibleColumns) {
                            Spacer(Modifier.size(cellSize))
                        }
                    }
                }
            }
        }
    }
}

fun homeLoadingTimelineRowSlotCounts(): List<Int> =
    (TIMELINE_ROWS - 1 downTo 0).map { visualRow ->
        timelineRowSlotIndices(visualRow).size
    }

@Composable
private fun ContactListSkeleton(brush: Brush) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(FosterTheme.colors.fill.quaternary)
            .padding(horizontal = 16.dp),
    ) {
        repeat(3) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier
                            .width(if (index == 1) 98.dp else 124.dp)
                            .height(17.dp),
                        shape = RoundedCornerShape(8.dp),
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    SkeletonBlock(
                        brush = brush,
                        modifier = Modifier
                            .width(74.dp)
                            .height(13.dp),
                        shape = RoundedCornerShape(6.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier
                        .width(76.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                )
            }
            if (index < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(FosterTheme.colors.stroke.secondary),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    brush: Brush,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}