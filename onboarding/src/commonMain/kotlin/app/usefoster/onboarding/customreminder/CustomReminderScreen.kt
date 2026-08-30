package app.usefoster.onboarding.customreminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.designsystem.buttons.FosterActionButton
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.onboarding.components.StepIndicator
import app.usefoster.onboarding.customreminder.components.AddReminderBottomSheet
import app.usefoster.onboarding.customreminder.components.CustomReminderCard
import app.usefoster.onboarding.presentation.rememberCustomReminderViewModel
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.theme.FosterTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.ic_add
import foster.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource
import foster.onboarding.generated.resources.action_next
import foster.onboarding.generated.resources.action_skip
import foster.onboarding.generated.resources.custom_add_more_title
import foster.onboarding.generated.resources.custom_like_birthdays
import foster.onboarding.generated.resources.reminders_add
import foster.onboarding.generated.resources.reminders_tap_plus
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomReminderScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberCustomReminderViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CustomReminderEvent.NavigateToNext -> onNavigateToNext()
                CustomReminderEvent.NavigateBack -> onBack()
                CustomReminderEvent.NavigateSkip -> onSkip()
            }
        }
    }

    CustomReminderScreenContent(
        state = state,
        onAction = { viewModel.onAction(it) },
        onNavigateToNext = { viewModel.onNextClicked() },
        onBack = { viewModel.onBackClicked() },
        onSkip = { viewModel.onSkipClicked() },
        modifier = modifier
    )
}

@Composable
private fun CustomReminderScreenContent(
    state: CustomReminderState,
    onAction: (CustomReminderAction) -> Unit,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liquidState = rememberLiquidState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // First-time affordance: the first reminder card nudges left once so the
    // user discovers the swipe-to-delete gesture. Survives recomposition but
    // resets per app session.
    var swipeHintPending by rememberSaveable { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FosterTheme.colors.background.b0)
                .liquefiable(liquidState)
        )

        val blurModifier = if (state.isBottomSheetVisible) Modifier.blur(20.dp) else Modifier

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(blurModifier),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FosterTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        StepIndicator(
                            totalSteps = 7,
                            currentStep = 4,
                        )
                    },
                    navigationIcon = { },
                    actions = {
                        Button(
                            onClick = onSkip,
                            colors = ButtonDefaults.buttonColors(containerColor = FosterTheme.colors.background.b0)
                        ) {
                            Text(
                                text = stringResource(Res.string.action_skip),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FosterTheme.colors.text.secondary,
                            )
                        }
                    },
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
                        FosterActionButton(
                            onClick = onBack,
                            leadingIcon = vectorResource(Res.drawable.ic_back),
                            modifier = modifier.weight(0.19f),
                        )
                        Spacer(Modifier.width(12.dp))
                        FosterButton(
                            text = stringResource(Res.string.action_next),
                            onClick = onNavigateToNext,
                            modifier = Modifier.weight(0.8f),
                        )
                    }
                }
            },
            containerColor = FosterTheme.colors.background.b0
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
                ) {
                    Spacer(Modifier.height(40.dp))

                    Text(
                        text = stringResource(Res.string.custom_add_more_title),
                        style = FosterTheme.typography.heading1Bold,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = FosterTheme.colors.text.primary,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(Res.string.custom_like_birthdays),
                        style = FosterTheme.typography.heading3,
                        fontWeight = FontWeight.Medium,
                        color = FosterTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(40.dp))

                if (state.reminders.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(54.dp))
                            FosterActionButton(
                                leadingIcon = vectorResource(Res.drawable.ic_add),
                                onClick = { onAction(CustomReminderAction.AddClicked) })
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(Res.string.reminders_add),
                                fontSize = 20.sp,
                                color = FosterTheme.colors.text.secondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(Res.string.reminders_tap_plus),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = FosterTheme.colors.text.tertiary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.reminders) { reminder ->
                            val isFirstCard = state.reminders.firstOrNull()?.id == reminder.id
                            CustomReminderCard(
                                reminder = reminder,
                                onClick = {
                                    onAction(CustomReminderAction.EditClicked(reminder.id))
                                },
                                onDelete = {
                                    onAction(CustomReminderAction.DeleteReminderClicked(reminder.id))
                                },
                                showSwipeHint = swipeHintPending && isFirstCard,
                                onSwipeHintShown = { swipeHintPending = false },
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val dividerColor = FosterTheme.colors.gray.quaternary
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .drawBehind {
                                        drawLine(
                                            color = dividerColor,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(10f, 10f), 0f
                                            )
                                        )
                                    }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            FosterActionButton(
                                leadingIcon = vectorResource(Res.drawable.ic_add),
                                onClick = { onAction(CustomReminderAction.AddClicked) })
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
            }
        }

        if (state.isBottomSheetVisible) {
            AddReminderBottomSheet(
                state = state,
                onAction = onAction
            )
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewCustomReminderScreen() {
    FosterTheme {
        CustomReminderScreen(
            onNavigateToNext = {},
            onBack = {},
            onSkip = {}
        )
    }
}
