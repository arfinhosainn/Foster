package app.usenekko.home.presentation.brainstorm.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.home.domain.BrainstormTopic
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_brainstorm
import org.jetbrains.compose.resources.vectorResource

@Composable
fun CurrentOutputContent(
    topics: List<BrainstormTopic>?,
    isGenerating: Boolean,
    notice: String?,
    error: String?,
    onGenerate: () -> Unit,
    onDismissNotice: () -> Unit,
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
            ErrorBanner(text = error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // The "generate" action — always available; the server enforces the
        // one-per-contact-per-day cooldown and we surface it as a notice.
        NekkoButton(
            text = "Generate ideas",
            onClick = onGenerate,
            loading = isGenerating,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Image(
                    imageVector = vectorResource(Res.drawable.ic_brainstorm),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        val current = topics
        if (current == null) {
            Text(
                text = "Personalized conversation ideas for this contact will appear here. Generated from your notes and relationship info.",
                style = NekkoTheme.typography.bodyMedium,
                color = NekkoTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )
        } else if (current.isEmpty()) {
            Text(
                text = "No ideas yet. Tap Generate ideas above.",
                style = NekkoTheme.typography.bodyMedium,
                color = NekkoTheme.colors.text.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = if (isGenerating) "Thinking of what to say…" else "Most recent ideas",
                style = NekkoTheme.typography.heading4Semibold,
                color = NekkoTheme.colors.text.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            current.forEachIndexed { index, topic ->
                TopicCard(topic = topic, index = index)
            }
        }
    }
}
