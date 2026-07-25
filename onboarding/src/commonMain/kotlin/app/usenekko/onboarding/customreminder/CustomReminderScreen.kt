package app.usenekko.onboarding.customreminder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.LiquidState
import app.usenekko.designsystem.topbar.NekkoTopAppBar
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.customreminder.components.AddReminderBottomSheet
import app.usenekko.onboarding.customreminder.components.CustomReminderCard
import app.usenekko.theme.NekkoTheme
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

    Box(modifier = modifier.fillMaxSize()) {

        // Source layer — sampled by AddButton's liquid effect.
        // Must stay a SIBLING of the content below, not an ancestor of it,
        // since AddButton (a descendant of the content Column) uses .liquid(liquidState).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NekkoTheme.colors.background.b0)
                .liquefiable(liquidState)
        )

        val blurModifier = if (state.isBottomSheetVisible) Modifier.blur(20.dp) else Modifier
        Column(modifier = Modifier.fillMaxSize().then(blurModifier)) {
            NekkoTopAppBar(
                trailingContent = {
                    Text(
                        text = "Skip",
                        color = NekkoTheme.colors.text.secondary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .clickable { onSkip() }
                    )
                }
            ) {
                StepIndicator(
                    totalSteps = 5,
                    currentStep = 3,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

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
                    style = NekkoTheme.typography.heading4,
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
                        AddButton(
                            liquidState = liquidState,
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
                        AddButton(
                            liquidState = liquidState,
                            onClick = { onAction(CustomReminderAction.AddClicked) })
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 24.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NekkoTheme.colors.fill.tertiary,
                        contentColor = NekkoTheme.colors.background.onBackground,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_back),
                        contentDescription = "Back",
                    )
                }
                Spacer(Modifier.width(12.dp))
                NekkoButton(
                    text = "Next",
                    onClick = onNavigateToNext,
                    modifier = Modifier.weight(1f),
                )
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

@Composable
private fun AddButton(liquidState: LiquidState, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent, // let .liquid() render the glass surface itself
        shadowElevation = 0.dp,
        modifier = Modifier.size(64.dp).liquid(liquidState) {
            frost = 16.dp
            shape = CircleShape
            tint = Color.White.copy(alpha = 0.2f)
            refraction = 0.3f
            saturation = 1.2f
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick), // no opaque background here
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_add),
                    contentDescription = "Add Reminder",
                    tint = NekkoTheme.colors.background.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
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