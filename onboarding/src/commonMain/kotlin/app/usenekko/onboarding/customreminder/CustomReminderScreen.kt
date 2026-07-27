package app.usenekko.onboarding.customreminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.customreminder.components.AddReminderBottomSheet
import app.usenekko.onboarding.customreminder.components.CustomReminderCard
import app.usenekko.onboarding.domain.CustomReminderDraft
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.ReminderFrequency
import app.usenekko.onboarding.presentation.LocalOnboardingDraftStore
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

@Composable
fun CustomReminderScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draftStore = LocalOnboardingDraftStore.current
    val draft by draftStore.draft.collectAsStateWithLifecycle()
    var sheetState by remember {
        mutableStateOf(
            CustomReminderState(reminders = draft.customReminders.map { it.toReminderItem() })
        )
    }
    val state = sheetState.copy(reminders = draft.customReminders.map { it.toReminderItem() })

    CustomReminderScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is CustomReminderAction.AddClicked -> {
                    sheetState = sheetState.copy(isBottomSheetVisible = true)
                }

                is CustomReminderAction.BottomSheetDismissed -> {
                    sheetState = sheetState.copy(isBottomSheetVisible = false)
                }

                is CustomReminderAction.DraftTitleChanged -> {
                    sheetState = sheetState.copy(draftTitle = action.title)
                }

                is CustomReminderAction.DraftDescriptionChanged -> {
                    sheetState = sheetState.copy(draftDescription = action.description)
                }

                is CustomReminderAction.DraftRecurrenceChanged -> {
                    sheetState = sheetState.copy(draftRecurrence = action.recurrence)
                }

                is CustomReminderAction.DraftDateChanged -> {
                    sheetState = sheetState.copy(draftDate = action.date)
                }

                is CustomReminderAction.SaveReminderClicked -> {
                    val newItem = CustomReminderDraft(
                        id = "rem_${draft.customReminders.size}",
                        title = state.draftTitle.ifEmpty { "New Reminder" },
                        description = state.draftDescription,
                        recurrence = state.draftRecurrence.toReminderFrequency(),
                    )
                    draftStore.update {
                        it.copy(
                            customReminders = it.customReminders + newItem,
                            currentStep = OnboardingStep.CustomReminder,
                        )
                    }
                    sheetState = sheetState.copy(
                        isBottomSheetVisible = false,
                        draftTitle = "",
                        draftDescription = "",
                        draftRecurrence = "None",
                        draftDate = "Choose Date"
                    )
                }
            }
        },
        onNavigateToNext = {
            draftStore.update { it.copy(currentStep = OnboardingStep.AddNote) }
            onNavigateToNext()
        },
        onBack = onBack,
        onSkip = {
            draftStore.update { it.copy(currentStep = OnboardingStep.AddNote) }
            onSkip()
        },
        modifier = modifier
    )
}

private fun CustomReminderDraft.toReminderItem(): ReminderItem = ReminderItem(
    id = id,
    title = title,
    description = description,
    recurrence = recurrence.toUiLabel(),
    date = dateEpochMillis?.toString() ?: "Choose Date",
)

private fun ReminderFrequency.toUiLabel(): String = when (this) {
    ReminderFrequency.Daily -> "Daily"
    ReminderFrequency.Weekly -> "Weekly"
    ReminderFrequency.BiWeekly -> "Bi-weekly"
    ReminderFrequency.Monthly -> "Monthly"
    ReminderFrequency.SemiAnnually -> "Semi-annually"
    ReminderFrequency.Annually -> "Yearly"
    ReminderFrequency.None -> "None"
}

private fun String.toReminderFrequency(): ReminderFrequency = when (this) {
    "Daily" -> ReminderFrequency.Daily
    "Weekly" -> ReminderFrequency.Weekly
    "Bi-weekly" -> ReminderFrequency.BiWeekly
    "Monthly" -> ReminderFrequency.Monthly
    "Semi-annually" -> ReminderFrequency.SemiAnnually
    "Yearly", "Annually" -> ReminderFrequency.Annually
    else -> ReminderFrequency.None
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

    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NekkoTheme.colors.background.b0)
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
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        StepIndicator(
                            totalSteps = 8,
                            currentStep = 5,
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(42.dp))

                    Text(
                        text = "Add more reminder",
                        style = NekkoTheme.typography.heading1Bold,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = NekkoTheme.colors.text.primary,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Like birthdays, anniversaries, etc",
                        style = NekkoTheme.typography.heading3,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.secondary,
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
                            Spacer(modifier = Modifier.height(40.dp))
                            NekkoActionButton(
                                leadingIcon = vectorResource(Res.drawable.ic_add),
                                onClick = { onAction(CustomReminderAction.AddClicked) })
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Add reminder",
                                fontSize = 20.sp,
                                color = NekkoTheme.colors.text.secondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap on the plus button",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = NekkoTheme.colors.text.tertiary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.reminders) { reminder ->
                            CustomReminderCard(reminder = reminder)
                            Spacer(modifier = Modifier.height(16.dp))
                            // Dashed divider line
                            val dividerColor = NekkoTheme.colors.gray.quaternary
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
                                                floatArrayOf(
                                                    10f,
                                                    10f
                                                ), 0f
                                            )
                                        )
                                    }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            NekkoActionButton(
                                leadingIcon = vectorResource(Res.drawable.ic_add),
                                onClick = { onAction(CustomReminderAction.AddClicked) })
                            Spacer(modifier = Modifier.height(24.dp))
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
    NekkoTheme {
        CustomReminderScreen(
            onNavigateToNext = {},
            onBack = {},
            onSkip = {}
        )
    }
}
