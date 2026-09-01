package app.usefoster.home.presentation.brainstorm.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.brainstorm_empty_current
import foster.home.generated.resources.brainstorm_empty_no_ideas
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CurrentOutputContent(
    topics: List<BrainstormTopic>?,
    isGenerating: Boolean,
    notice: String?,
    error: StringResource?,
    onDismissNotice: () -> Unit,
    onSendMessage: ((BrainstormTopic) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (notice != null) {
            NoticeBanner(text = notice, onDismiss = onDismissNotice)
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (error != null) {
            ErrorBanner(text = stringResource(error))
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isGenerating) {

            Spacer(modifier = Modifier.height(12.dp))
            repeat(SHIMMER_CARD_COUNT) {
                ShimmerTopicCard()
            }
        } else if (topics == null) {
            Text(
                text = stringResource(Res.string.brainstorm_empty_current),
                style = FosterTheme.typography.bodyMedium,
                color = FosterTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )
        } else if (topics.isEmpty()) {
            Text(
                text = stringResource(Res.string.brainstorm_empty_no_ideas),
                style = FosterTheme.typography.bodyMedium,
                color = FosterTheme.colors.text.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {

            Spacer(modifier = Modifier.height(12.dp))
            topics.forEach { topic ->
                TopicCard(
                    topic = topic,
                    onSendMessage = onSendMessage?.let { send -> { send(topic) } },
                )
            }
        }
    }
}

private const val SHIMMER_CARD_COUNT = 3
