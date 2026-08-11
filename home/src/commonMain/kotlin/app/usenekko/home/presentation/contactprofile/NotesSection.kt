package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import nekko.home.generated.resources.ic_flower_light
import nekko.home.generated.resources.ic_flower_night
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

private val NOTE_CARD_HEIGHT = 182.dp
private val NOTE_MENU_WIDTH = 88.dp

@Composable
fun NotesSection(
    notes: List<Note>,
    onAddNote: () -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowLargeAddNoteCard(notes)) {
        NotesEmptyState(
            onAddNote = onAddNote,
            modifier = modifier.padding(horizontal = 30.dp),
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            noteGridRows(notes).forEach { rowNotes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowNotes.forEach { note ->
                        NoteCard(
                            note = note,
                            onDelete = { onDeleteNote(note.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowNotes.size == 1) {
                        AddNoteCard(
                            onClick = onAddNote,
                            modifier = Modifier
                                .weight(1f)
                                .height(NOTE_CARD_HEIGHT),
                        )
                    }
                }
            }
            if (!shouldPlaceAddNoteBesideLastNote(notes)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AddNoteCard(
                        onClick = onAddNote,
                        modifier = Modifier
                            .weight(1f)
                            .height(NOTE_CARD_HEIGHT),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

fun shouldShowLargeAddNoteCard(notes: List<Note>): Boolean = notes.isNotEmpty()

fun shouldPlaceAddNoteBesideLastNote(notes: List<Note>): Boolean = notes.size % 2 == 1

fun noteGridRows(notes: List<Note>): List<List<Note>> = notes.chunked(2)

@Composable
private fun NoteCard(
    note: Note,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cardColor = NekkoTheme.colors.fill.quaternary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(NOTE_CARD_HEIGHT)
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.quaternary)
            .padding(20.dp),
    ) {
        Text(
            text = note.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Text(
                text = note.body,
                maxLines = 6,
                overflow = TextOverflow.Clip,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = NekkoTheme.colors.text.tertiary,
            )

            // Large fade mask matching the card background.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.38f to Color.Transparent,
                                0.78f to cardColor.copy(alpha = 0.92f),
                                1.00f to cardColor,
                            ),
                        ),
                    ),
            )

            // Footer is drawn after the fade, so it remains visible.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatNoteDate(note.createdAt),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary,
                )

                Box {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Note options",
                        tint = NekkoTheme.colors.text.tertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { menuExpanded = true },
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .width(NOTE_MENU_WIDTH)
                            .background(NekkoTheme.colors.background.b0),
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.height(36.dp),
                            text = {
                                Text(
                                    text = "Delete",
                                    color = NekkoTheme.colors.text.primary,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
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
        Image(
            painter = painterResource(
                if (isSystemInDarkTheme()) {
                    Res.drawable.ic_flower_night
                } else {
                    Res.drawable.ic_flower_light
                },
            ),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun AddNoteCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.quaternary)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_add),
            contentDescription = null,
            tint = NekkoTheme.colors.text.tertiary,
            modifier = Modifier
                .size(20.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Add Note",
            color = NekkoTheme.colors.text.tertiary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
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
