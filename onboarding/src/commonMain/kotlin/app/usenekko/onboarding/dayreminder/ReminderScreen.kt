package app.usenekko.onboarding.dayreminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
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
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.dayreminder.components.ReminderOptionCard
import app.usenekko.onboarding.presentation.rememberReminderViewModel
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource
import nekko.onboarding.generated.resources.action_next
import nekko.onboarding.generated.resources.add_step_frequency_subtitle
import nekko.onboarding.generated.resources.add_step_frequency_title
import org.jetbrains.compose.resources.stringResource
import nekko.onboarding.generated.resources.add_freq_annually
import nekko.onboarding.generated.resources.add_freq_biweekly
import nekko.onboarding.generated.resources.add_freq_daily
import nekko.onboarding.generated.resources.add_freq_monthly
import nekko.onboarding.generated.resources.add_freq_semiannually
import nekko.onboarding.generated.resources.add_freq_weekly

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
            AdaptiveSurface {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NekkoActionButton(
                        onClick = onBack,
                        leadingIcon = vectorResource(Res.drawable.ic_back),
                        modifier = modifier.weight(0.19f),
                    )
                    Spacer(Modifier.width(12.dp))
                    NekkoButton(
                        text = stringResource(Res.string.action_next),
                        onClick = onNavigateToNext,
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }
        },
        containerColor = NekkoTheme.colors.background.b0
    ) { innerPadding ->
        AdaptiveSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(40.dp))

                Text(
                    text = stringResource(Res.string.add_step_frequency_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                    lineHeight = 36.sp,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.add_step_frequency_subtitle),
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReminderOptions.all) { option ->
                    ReminderOptionCard(
                        text = stringResource(optionLabels.getValue(option)),
                        isSelected = option == state.selectedOption,
                        onClick = { onAction(ReminderAction.SelectOption(option)) }
                    )
                }
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

/** UI label resources for the canonical frequency option constants. */
private val optionLabels = mapOf(
    ReminderOptions.DAILY to Res.string.add_freq_daily,
    ReminderOptions.WEEKLY to Res.string.add_freq_weekly,
    ReminderOptions.BI_WEEKLY to Res.string.add_freq_biweekly,
    ReminderOptions.MONTHLY to Res.string.add_freq_monthly,
    ReminderOptions.SEMI_ANNUALLY to Res.string.add_freq_semiannually,
    ReminderOptions.ANNUALLY to Res.string.add_freq_annually,
)
