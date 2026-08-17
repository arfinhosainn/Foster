package app.usenekko.onboarding.addnote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
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
import app.usenekko.onboarding.presentation.rememberAddNoteViewModel
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
import nekko.onboarding.generated.resources.ic_back
import nekko.onboarding.generated.resources.ic_flower
import org.jetbrains.compose.resources.vectorResource

@Composable
fun AddNoteScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberAddNoteViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddNoteEvent.NavigateToNext -> onNavigateToNext()
                AddNoteEvent.NavigateBack -> onBack()
                AddNoteEvent.NavigateSkip -> onSkip()
                is AddNoteEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddNoteScreenContent(
        state = state,
        onAction = { viewModel.onAction(it) },
        onNavigateToNext = { viewModel.onNavigateToNext() },
        onBack = { viewModel.onBack() },
        onSkip = { viewModel.onSkip() },
        snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState,
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            totalSteps = 8,
                            currentStep = 6,
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
                        loading = state.isSubmitting,
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
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = "Add a Note",
                        style = NekkoTheme.typography.heading1Bold,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = NekkoTheme.colors.text.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Capture thoughts & memories\nabout your conversation",
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
                            Spacer(modifier = Modifier.height(69.dp))
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_flower),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(74.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Capture thoughts & memories\nabout your conversation",
                                fontSize = 15.sp,
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

                    Spacer(Modifier.height(40.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(state.notes) { note ->
                            NoteCard(
                                note = note,
                                onDelete = { onAction(AddNoteAction.DeleteNote(note.id)) },
                                onClick = { onAction(AddNoteAction.EditClicked(note.id)) },
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
