package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.home.di.rememberContactProfileViewModel
import app.usenekko.theme.NekkoTheme

@Composable
fun ContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberContactProfileViewModel(contactId)
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Contact no longer exists (or the lookup failed) — pop back to Home instead
    // of rendering a broken screen.
    val notFound = !state.isLoading && state.contact == null
    LaunchedEffect(notFound) {
        if (notFound) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ContactProfileTopBar(
                daysUntilNextCheckIn = state.daysUntilNextCheckIn,
                onBack = onBack,
                onEditClick = {}, // no-op — no edit screen yet, deferred
            )
        },
        containerColor = NekkoTheme.colors.background.b0,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            state.contact?.let { c ->
                ContactProfileHeader(
                    name = c.name,
                    frequencyLabel = formatFrequencyLabel(c.checkInFrequency),
                    reminderTime = formatReminderTime(c.reminderTime),
                    isExpanded = state.isRelationshipInfoOpen,
                    onNameClick = { viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo) },
                    onNotificationClick = { /* TODO: mute/notification toggle — not built yet */ },
                    onCheckInClick = { viewModel.onAction(ContactProfileAction.CheckIn) },
                )
            }
            // TODO: notes section (empty-state placeholder per earlier scope),
            // TODO: RelationshipInfoBottomSheet when state.isRelationshipInfoOpen
        }
    }
}

private fun formatFrequencyLabel(frequency: String): String = when (frequency) {
    "daily" -> "Daily"
    "weekly" -> "Weekly"
    "biweekly" -> "Bi-Weekly"
    "monthly" -> "Monthly"
    else -> "No schedule"
}

private fun formatReminderTime(time: String?): String {
    if (time == null) return ""
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.toString() ?: return ""
    val minute = parts.getOrNull(1) ?: return ""
    return "$hour:$minute"
}
