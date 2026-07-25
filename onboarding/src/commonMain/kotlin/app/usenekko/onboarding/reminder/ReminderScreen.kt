package app.usenekko.onboarding.reminder

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.topbar.NekkoTopAppBar
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.reminder.components.ReminderOptionCard
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
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
    var state by remember { mutableStateOf(ReminderState()) }

    ReminderScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is ReminderAction.SelectOption -> {
                    state = state.copy(selectedOption = action.option)
                }
            }
        },
        onNavigateToNext = onNavigateToNext,
        onBack = onBack,
        modifier = modifier
    )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
    ) {
        // ── Top bar with step indicator ─────────────────────────────────
        NekkoTopAppBar {
            StepIndicator(
                totalSteps = 5,
                currentStep = 3, // Displaying 4th dot as active
            )
        }

        // ── Title section ───────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Every day is precious",
                style = NekkoTheme.typography.heading1Bold,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = NekkoTheme.colors.text.primary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "How often do you want to be\nreminded?",
                style = NekkoTheme.typography.heading3,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Options list ────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ReminderOptions.all) { option ->
                ReminderOptionCard(
                    text = option,
                    isSelected = option == state.selectedOption,
                    onClick = { onAction(ReminderAction.SelectOption(option)) }
                )
            }
        }

        // ── Bottom navigation buttons ───────────────────────────────────
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
                    contentColor = NekkoTheme.colors.background.onBackground
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
