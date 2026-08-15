package app.usenekko.home.presentation.brainstorm.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.usenekko.home.domain.BrainstormSession
import app.usenekko.theme.NekkoTheme
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

@Composable
fun HistoryContent(
    sessions: List<BrainstormSession>,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        when {
            isLoading -> HistoryLoadingSkeleton()
            error != null -> ErrorBanner(text = error, modifier = Modifier.padding(top = 16.dp))
            sessions.isEmpty() -> Text(
                text = "No past brainstorms yet. Generate your first batch of ideas in the Current Output tab.",
                style = NekkoTheme.typography.bodyMedium,
                color = NekkoTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            )
            else -> Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                groupByDate(sessions).forEach { (label, groupSessions) ->
                    Text(
                        text = label,
                        style = NekkoTheme.typography.heading4Semibold,
                        color = NekkoTheme.colors.text.tertiary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    )
                    groupSessions.forEach { session ->
                        if (session.topics.isEmpty()) {
                            Text(
                                text = "A brainstorm session from earlier",
                                style = NekkoTheme.typography.bodyMedium,
                                color = NekkoTheme.colors.text.tertiary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            session.topics.forEachIndexed { index, topic ->
                                TopicCard(topic = topic, index = index)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "brainstormHistoryShimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
        ),
        label = "brainstormHistoryShimmerPosition",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            NekkoTheme.colors.fill.quaternary,
            NekkoTheme.colors.fill.secondary,
            NekkoTheme.colors.fill.quaternary,
        ),
        start = Offset(shimmerPosition * 500f, 0f),
        end = Offset((shimmerPosition + 1f) * 500f, 500f),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        repeat(2) { sectionIndex ->
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 12.dp)
                    .width(if (sectionIndex == 0) 76.dp else 112.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush),
            )
            repeat(if (sectionIndex == 0) 3 else 2) {
                ShimmerTopicCard()
            }
        }
    }
}

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun sessionDateLabel(createdAt: String): String {
    val date = runCatching {
        Instant.parse(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }.getOrNull() ?: return createdAt
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (date) {
        today -> "Today"
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> "${MONTHS[date.month.ordinal]} ${date.day}, ${date.year}"
    }
}

// Sessions arrive newest-first; group by their (local) day label, preserving order.
private fun groupByDate(sessions: List<BrainstormSession>): List<Pair<String, List<BrainstormSession>>> {
    val result = mutableListOf<Pair<String, List<BrainstormSession>>>()
    var label: String? = null
    var buffer = mutableListOf<BrainstormSession>()
    sessions.forEach { session ->
        val currentLabel = sessionDateLabel(session.createdAt)
        if (currentLabel != label) {
            if (label != null) result.add(label to buffer)
            label = currentLabel
            buffer = mutableListOf()
        }
        buffer.add(session)
    }
    if (label != null) result.add(label to buffer)
    return result
}
