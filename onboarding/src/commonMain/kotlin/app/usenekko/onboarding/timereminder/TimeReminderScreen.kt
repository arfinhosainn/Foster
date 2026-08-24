package app.usenekko.onboarding.timereminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import app.usenekko.onboarding.presentation.rememberTimeReminderViewModel
import app.usenekko.onboarding.timereminder.components.AmPmToggle
import app.usenekko.onboarding.timereminder.components.TimeScrollDial
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource
import nekko.onboarding.generated.resources.action_next
import nekko.onboarding.generated.resources.action_skip
import nekko.onboarding.generated.resources.add_step_time_subtitle
import nekko.onboarding.generated.resources.add_step_time_title
import nekko.onboarding.generated.resources.cd_back_upper
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimeReminderScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberTimeReminderViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TimeReminderEvent.NavigateToNext -> onNavigateToNext()
                TimeReminderEvent.NavigateBack -> onBack()
                TimeReminderEvent.NavigateSkip -> onSkip()
            }
        }
    }

    TimeReminderScreenContent(
        state = state,
        onAction = { viewModel.onAction(it) },
        onNavigateToNext = { viewModel.onNextClicked() },
        onBack = { viewModel.onBackClicked() },
        modifier = modifier,
        onSkip = { viewModel.onSkipClicked() }
    )
}

@Composable
private fun TimeReminderScreenContent(
    state: TimeReminderState,
    onAction: (TimeReminderAction) -> Unit,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
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
                        currentStep = 3,
                    )
                },
                actions = {
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.buttonColors(containerColor = NekkoTheme.colors.background.b0)
                    ) {
                        Text(
                            text = stringResource(Res.string.action_skip),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NekkoTheme.colors.text.secondary,
                        )
                    }
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
                    modifier = Modifier
                        .weight(0.23f)
                        .size(58.dp),
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = NekkoTheme.colors.fill.tertiary,
                    ),
                ){
                    Image(
                        imageVector = vectorResource(Res.drawable.ic_back),
                        contentDescription = stringResource(Res.string.cd_back_upper)
                    )
                }
                Spacer(Modifier.width(12.dp))
                NekkoButton(
                    text = stringResource(Res.string.action_next),
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
            ) {
                Spacer(Modifier.height(40.dp))

                Text(
                    text = stringResource(Res.string.add_step_time_title),
                    style = NekkoTheme.typography.heading1Bold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.add_step_time_subtitle),
                    style = NekkoTheme.typography.heading3,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(54.dp))

            TimeScrollDial(
                totalMinutes = state.totalMinutes,
                onValueChange = { newTotal ->
                    onAction(TimeReminderAction.ScrollToMinute(newTotal))
                },
            )

            Spacer(Modifier.height(24.dp))

            AmPmToggle(
                isAm = state.isAm,
                onToggle = { isAm -> onAction(TimeReminderAction.ToggleAmPm(isAm)) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@PreviewLightDark
@Composable
private fun TimeReminderScreenPreview() {
    NekkoTheme {
        TimeReminderScreen(
            onNavigateToNext = {},
            onBack = {},
            onSkip = {}
        )
    }
}
