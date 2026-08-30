package app.usefoster.onboarding.addnote.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.onboarding.addnote.NoteItem
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.action_delete
import foster.onboarding.generated.resources.cd_menu
import foster.onboarding.generated.resources.note_weekly_reflection
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoteCard(
    note: NoteItem,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(FosterTheme.colors.fill.quaternary)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = note.title,
                style = FosterTheme.typography.heading4Semibold,
                color = FosterTheme.colors.text.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.height(60.dp)) {
                Text(
                    text = note.description,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    style = FosterTheme.typography.heading4,
                    color = FosterTheme.colors.text.tertiary,
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
                                    FosterTheme.colors.fill.quaternary,
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
                    color = FosterTheme.colors.text.tertiary,
                )
                Box {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(Res.string.cd_menu),
                        tint = FosterTheme.colors.text.tertiary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { menuExpanded = true }
                            .padding(4.dp)
                            .size(20.dp),
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(FosterTheme.colors.background.b0),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(Res.string.action_delete),
                                    color = FosterTheme.colors.text.primary,
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
    FosterTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = NoteItem(
                    title = stringResource(Res.string.note_weekly_reflection),
                    description = "This week I focused on improving my coding skills and learning new technologies like Jetpack Compose. It was a productive week with many challenges overcome.",
                    date = "Oct 24, 2023",
                    id = "34"
                ),
                onDelete = {},
                onClick = {}
            )
        }
    }
}
