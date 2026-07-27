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
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderFrequency
import app.usenekko.onboarding.presentation.LocalOnboardingDraftStore
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

/**
 * Stateful entry point for ReminderScreen.
 */
@Composable
fun ReminderScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draftStore = LocalOnboardingDraftStore.current
    val draft by draftStore.draft.collectAsStateWithLifecycle()
    val state = ReminderState(
        selectedOption = draft.reminderFrequency.toReminderOption(),
    )

    ReminderScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is ReminderAction.SelectOption -> {
                    draftStore.update {
                        it.copy(
                            reminderFrequency = action.option.toReminderFrequency(),
                            currentStep = OnboardingStep.DayReminder,
                        )
                    }
                }
            }
        },
        onNavigateToNext = {
            draftStore.update { it.copy(currentStep = OnboardingStep.TimeReminder) }
            onNavigateToNext()
        },
        onBack = onBack,
        modifier = modifier
    )
}

private fun ReminderFrequency?.toReminderOption(): String = when (this) {
    ReminderFrequency.Daily -> ReminderOptions.DAILY
    ReminderFrequency.Weekly -> ReminderOptions.WEEKLY
    ReminderFrequency.BiWeekly -> ReminderOptions.BI_WEEKLY
    ReminderFrequency.Monthly -> ReminderOptions.MONTHLY
    ReminderFrequency.SemiAnnually -> ReminderOptions.SEMI_ANNUALLY
    ReminderFrequency.Annually -> ReminderOptions.ANNUALLY
    ReminderFrequency.None, null -> ReminderOptions.DAILY
}

private fun String.toReminderFrequency(): ReminderFrequency = when (this) {
    ReminderOptions.DAILY -> ReminderFrequency.Daily
    ReminderOptions.WEEKLY -> ReminderFrequency.Weekly
    ReminderOptions.BI_WEEKLY -> ReminderFrequency.BiWeekly
    ReminderOptions.MONTHLY -> ReminderFrequency.Monthly
    ReminderOptions.SEMI_ANNUALLY -> ReminderFrequency.SemiAnnually
    ReminderOptions.ANNUALLY -> ReminderFrequency.Annually
    else -> ReminderFrequency.None
}

/**
 * Stateless content for ReminderScreen.
 */
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
                        totalSteps = 8,
                        currentStep = 3,
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
            // ── Title section ───────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(42.dp))

                Text(
                    text = "Every day is precious",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                    lineHeight = 36.sp,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "How often do you want to be\nreminded?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 30.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
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
