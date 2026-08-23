package app.usenekko.onboarding.dayreminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.dayreminder.components.ReminderOptionCard
import app.usenekko.onboarding.presentation.rememberReminderViewModel
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ReminderScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberReminderViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ReminderEvent.NavigateToNext -> onNavigateToNext()
                ReminderEvent.NavigateBack -> onBack()
            }
        }
    }

    ReminderScreenContent(
        state = state,
        onAction = { viewModel.onAction(it) },
        onNavigateToNext = { viewModel.onNextClicked() },
        onBack = { viewModel.onBackClicked() },
        modifier = modifier
    )
}

@Composable
private fun ReminderScreenContent(
    state: ReminderState,
    onAction: (ReminderAction) -> Unit,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NekkoTheme.colors.background.b0,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    StepIndicator(
                        totalSteps = 7,
                        currentStep = 2,
                    )
                },
                navigationIcon = { },
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
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(40.dp))

                Text(
                    text = "Every day is precious",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                    lineHeight = 36.sp,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "How often do you want to be\nreminded?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(40.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 30.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReminderOptions.all) { option ->
                    ReminderOptionCard(
                        text = option,
                        isSelected = option == state.selectedOption,
                        onClick = { onAction(ReminderAction.SelectOption(option)) }
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ReminderScreenPreview() {
    NekkoTheme {
        ReminderScreen(
            onNavigateToNext = {},
            onBack = {}
        )
    }
}
