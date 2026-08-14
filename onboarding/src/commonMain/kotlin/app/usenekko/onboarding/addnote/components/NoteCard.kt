package app.usenekko.onboarding.addnote.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.onboarding.addnote.NoteItem
import app.usenekko.theme.NekkoTheme

@Composable
fun NoteCard(
    note: NoteItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.quaternary)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = note.title,
                style = NekkoTheme.typography.heading4Semibold,
                color = NekkoTheme.colors.text.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.height(60.dp)) {
                Text(
                    text = note.description,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = NekkoTheme.typography.heading4,
                    color = NekkoTheme.colors.text.tertiary,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    NekkoTheme.colors.fill.quaternary,
                                ),
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = note.date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary,
                )
                Box {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Menu",
                        tint = NekkoTheme.colors.text.tertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(0.dp),
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(NekkoTheme.colors.background.b0),
                    ) {
                        DropdownMenuItem(
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

@PreviewLightDark
@Composable
private fun NoteCardPreview() {
    NekkoTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = NoteItem(
                    title = "Weekly Reflection",
                    description = "This week I focused on improving my coding skills and learning new technologies like Jetpack Compose. It was a productive week with many challenges overcome.",
                    date = "Oct 24, 2023",
                    id = "34"
                ),
                onDelete = {}
            )
        }
    }
}
