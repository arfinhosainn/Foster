package app.usenekko.onboarding.customreminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var state by remember { mutableStateOf(CustomReminderState()) }

    CustomReminderScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is CustomReminderAction.AddClicked -> {
                    state = state.copy(isBottomSheetVisible = true)
                }

                is CustomReminderAction.BottomSheetDismissed -> {
                    state = state.copy(isBottomSheetVisible = false)
                }

                is CustomReminderAction.DraftTitleChanged -> {
                    state = state.copy(draftTitle = action.title)
                }

                is CustomReminderAction.DraftDescriptionChanged -> {
                    state = state.copy(draftDescription = action.description)
                }

                is CustomReminderAction.DraftRecurrenceChanged -> {
                    state = state.copy(draftRecurrence = action.recurrence)
                }

                is CustomReminderAction.DraftDateChanged -> {
                    state = state.copy(draftDate = action.date)
                }

                is CustomReminderAction.SaveReminderClicked -> {
                    val newItem = ReminderItem(
                        id = "rem_${state.reminders.size}",
                        title = state.draftTitle.ifEmpty { "New Reminder" },
                        description = state.draftDescription,
                        recurrence = state.draftRecurrence,
                        date = state.draftDate
                    )
                    state = state.copy(
                        reminders = state.reminders + newItem,
                        isBottomSheetVisible = false,
                        draftTitle = "",
                        draftDescription = "",
                        draftRecurrence = "None",
                        draftDate = "Choose Date"
                    )
                }
            }
        },
        onNavigateToNext = onNavigateToNext,
        onBack = onBack,
        onSkip = onSkip,
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
                            totalSteps = 5,
                            currentStep = 3,
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

                    Spacer(Modifier.height(6.dp))

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
