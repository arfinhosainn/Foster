package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.home.domain.Note
import app.usenekko.theme.NekkoTheme
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_add
import org.jetbrains.compose.resources.vectorResource

@Composable
fun NotesSection(
    notes: List<Note>,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (notes.isEmpty()) {
        NotesEmptyState(
            onAddNote = onAddNote,
            modifier = modifier.padding(horizontal = 30.dp),
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            notes.forEach { note ->
                NoteCard(note = note)
            }
            Spacer(modifier = Modifier.height(8.dp))
            NekkoActionButton(
                text = "Add Note",
                leadingIcon = vectorResource(Res.drawable.ic_add),
                onClick = onAddNote,
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.quaternary)
            .padding(20.dp),
    ) {
        Text(
            text = note.title,
            style = NekkoTheme.typography.heading4Semibold,
            color = NekkoTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatNoteDate(note.createdAt),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun NotesEmptyState(
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Capture thoughts & memories\nabout your conversation",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        NekkoActionButton(
            text = "Add Note",
            leadingIcon = vectorResource(Res.drawable.ic_add),
            onClick = onAddNote,
        )
    }
}

private fun formatNoteDate(createdAt: String): String {
    val localDate = runCatching {
        Instant.parse(createdAt)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }.getOrNull() ?: return createdAt
    val monthAbbr = localDate.month.name.lowercase()
        .replaceFirstChar { it.uppercaseChar() }
        .take(3)
    return "$monthAbbr ${localDate.day}, ${localDate.year}"
}
