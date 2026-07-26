package app.usenekko.onboarding.addnote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.addnote.components.AddNoteBottomSheet
import app.usenekko.onboarding.addnote.components.NoteCard
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.theme.NekkoTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
import nekko.onboarding.generated.resources.ic_back
import nekko.onboarding.generated.resources.ic_flower
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Clock

@Composable
fun AddNoteScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(AddNoteState()) }

    AddNoteScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is AddNoteAction.AddClicked -> {
                    state = state.copy(isBottomSheetVisible = true)
                }

                is AddNoteAction.BottomSheetDismissed -> {
                    state = state.copy(isBottomSheetVisible = false)
                }

                is AddNoteAction.DraftTitleChanged -> {
                    state = state.copy(draftTitle = action.title)
                }

                is AddNoteAction.DraftDescriptionChanged -> {
                    state = state.copy(draftDescription = action.description)
                }

                is AddNoteAction.SaveClicked -> {
                    val newNote = NoteItem(
                        id = "note_${state.notes.size}",
                        title = state.draftTitle.ifEmpty { "Untitled" },
                        description = state.draftDescription,
                        date = currentFormattedDate(),
                    )
                    state = state.copy(
                        notes = state.notes + newNote,
                        isBottomSheetVisible = false,
                        draftTitle = "",
                        draftDescription = "",
                    )
                }

                is AddNoteAction.DeleteNote -> {
                    state = state.copy(
                        notes = state.notes.filter { it.id != action.id },
                    )
                }
            }
        },
        onNavigateToNext = onNavigateToNext,
        onBack = onBack,
        onSkip = onSkip,
        modifier = modifier,
    )
}

@Composable
private fun AddNoteScreenContent(
    state: AddNoteState,
    onAction: (AddNoteAction) -> Unit,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NekkoTheme.colors.background.b0),
        )

        val blurModifier = if (state.isBottomSheetVisible) Modifier.blur(20.dp) else Modifier

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(blurModifier),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        StepIndicator(
                            totalSteps = 6,
                            currentStep = 2,
                        )
                    },
                    navigationIcon = { },
                    actions = {
                        Button(
                            onClick = onSkip,
                            colors = ButtonDefaults.buttonColors(containerColor = NekkoTheme.colors.background.b0)
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NekkoTheme.colors.text.secondary,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .padding(bottom = 24.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledIconButton(
                        modifier = modifier.weight(0.23f).size(58.dp),
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = NekkoTheme.colors.fill.tertiary)
                    ) {
                        Image(
                            imageVector = vectorResource(Res.drawable.ic_back),
                            contentDescription = "BACK"
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    NekkoButton(
                        text = "Next",
                        onClick = onNavigateToNext,
                        modifier = Modifier.weight(0.8f),
                    )
                }
            },
            containerColor = NekkoTheme.colors.background.b0
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(42.dp))
                    Text(
                        text = "Add a Note",
                        style = NekkoTheme.typography.heading1Bold,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = NekkoTheme.colors.text.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Capture thoughts & memories\nabout your conversation",
                        style = NekkoTheme.typography.heading4,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(40.dp))

                if (state.notes.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(40.dp))
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_flower),
                                contentDescription = null,
                                tint = Color.Unspecified,
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
                                onClick = { onAction(AddNoteAction.AddClicked) },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(state.notes) { note ->
                            NoteCard(
                                note = note,
                                onDelete = { onAction(AddNoteAction.DeleteNote(note.id)) },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            NekkoActionButton(
                                onClick = { onAction(AddNoteAction.AddClicked) },
                            )
                        }
                    }
                }
            }
        }

        if (state.isBottomSheetVisible) {
            AddNoteBottomSheet(
                state = state,
                onAction = onAction,
            )
        }
    }
}


private fun currentFormattedDate(): String {
    val now = Clock.System.now()
    val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dayOfWeek = localDate.dayOfWeek.name.take(3)
    val day = localDate.day.toString().padStart(2, '0')
    val month = localDate.month.name.take(3)
    val year = (localDate.year % 100).toString().padStart(2, '0')
    return "$dayOfWeek, $day $month $year"
}

@PreviewLightDark
@Composable
fun PreviewAddNoteScreen() {
    NekkoTheme {
        AddNoteScreen(
            onNavigateToNext = {},
            onBack = {},
            onSkip = {},
        )

    }
}
