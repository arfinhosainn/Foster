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
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderTimeDraft
import app.usenekko.onboarding.presentation.LocalOnboardingDraftStore
import app.usenekko.onboarding.timereminder.components.AmPmToggle
import app.usenekko.onboarding.timereminder.components.TimeScrollDial
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

/**
 * Stateful entry point for the Time Reminder screen.
 */
@Composable
fun TimeReminderScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draftStore = LocalOnboardingDraftStore.current
    val draft by draftStore.draft.collectAsStateWithLifecycle()
    val state = draft.reminderTime.toTimeReminderState()

    TimeReminderScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is TimeReminderAction.ScrollToMinute -> {
                    val hour = if (action.totalMinutes / 60 == 0) 12 else action.totalMinutes / 60
                    val minute = action.totalMinutes % 60
                    draftStore.update {
                        it.copy(
                            reminderTime = TimeReminderState(
                                selectedHour = hour,
                                selectedMinute = minute,
                                isAm = state.isAm,
                            ).toReminderTimeDraft(),
                            currentStep = OnboardingStep.TimeReminder,
                        )
                    }
                }

                is TimeReminderAction.ToggleAmPm -> {
                    draftStore.update {
                        it.copy(
                            reminderTime = state.copy(isAm = action.isAm).toReminderTimeDraft(),
                            currentStep = OnboardingStep.TimeReminder,
                        )
                    }
                }
            }
        },
        onNavigateToNext = {
            draftStore.update { it.copy(currentStep = OnboardingStep.CustomReminder) }
            onNavigateToNext()
        },
        onBack = onBack,
        modifier = modifier,
        onSkip = {
            draftStore.update { it.copy(currentStep = OnboardingStep.CustomReminder) }
            onSkip()
        }
    )
}

private fun ReminderTimeDraft?.toTimeReminderState(): TimeReminderState {
    if (this == null) return TimeReminderState()
    val periodHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return TimeReminderState(
        selectedHour = periodHour,
        selectedMinute = minute,
        isAm = hour < 12,
    )
}

private fun TimeReminderState.toReminderTimeDraft(): ReminderTimeDraft {
    val hour24 = when {
        isAm && selectedHour == 12 -> 0
        isAm -> selectedHour
        selectedHour == 12 -> 12
        else -> selectedHour + 12
    }
    return ReminderTimeDraft(hour = hour24, minute = selectedMinute)
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
                        totalSteps = 8,
                        currentStep = 4,
                    )
                },
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
            // ── Title section ───────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(42.dp))

                Text(
                    text = "Choose reminder time",
                    style = NekkoTheme.typography.heading1Bold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "How often do you want to be reminded?",
                    style = NekkoTheme.typography.heading3,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Time dial ───────────────────────────────────────────────
            TimeScrollDial(
                totalMinutes = state.totalMinutes,
                onValueChange = { newTotal ->
                    onAction(TimeReminderAction.ScrollToMinute(newTotal))
                },
            )

            Spacer(Modifier.height(24.dp))

            // ── AM / PM toggle ──────────────────────────────────────────
            AmPmToggle(
                isAm = state.isAm,
                onToggle = { isAm -> onAction(TimeReminderAction.ToggleAmPm(isAm)) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // Push bottom nav to the bottom
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
