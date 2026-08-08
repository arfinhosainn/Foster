package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.home.di.rememberContactProfileViewModel
import app.usenekko.theme.NekkoTheme

@Composable
fun ContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    onBrainstormClick: () -> Unit,
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            state.contact?.let { c ->
                ContactProfileHeader(
                    name = c.name,
                    frequencyLabel = formatFrequencyLabel(c.checkInFrequency),
                    reminderTime = formatReminderTime(c.reminderTime),
                    isExpanded = state.isRelationshipInfoOpen,
                    onNameClick = { viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo) },
                    onNotificationClick = { /* TODO: mute/notification toggle — not built yet */ },
                    onCheckInClick = { viewModel.onAction(ContactProfileAction.CheckIn) },
                    onBrainstormClick = onBrainstormClick,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            NotesSection(
                notes = state.notes,
                onAddNote = { viewModel.onAction(ContactProfileAction.OpenAddNote) },
                onDeleteNote = { viewModel.onAction(ContactProfileAction.DeleteNote(it)) },
            )
            // TODO: RelationshipInfoBottomSheet when state.isRelationshipInfoOpen
        }
    }

    if (state.isRelationshipInfoOpen) {
        RelationshipInfoSheet(
            checkInCount = state.checkInCount,
            nextCheckInDate = state.contact?.nextCheckInDate,
            reminders = state.reminders,
            remindersError = state.remindersError,
            onDismiss = { viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo) },
            onAddReminder = { viewModel.onAction(ContactProfileAction.OpenAddReminder) },
            onEditReminder = { viewModel.onAction(ContactProfileAction.EditReminder(it)) },
            onDeleteReminder = { viewModel.onAction(ContactProfileAction.DeleteReminder(it)) },
        )
    }

    if (state.isAddReminderSheetOpen) {
        AddReminderSheet(
            draftTitle = state.reminderDraftTitle,
            draftDescription = state.reminderDraftDescription,
            draftRecurrence = state.reminderDraftRecurrence,
            draftDateEpochMillis = state.reminderDraftDateEpochMillis,
            isSaving = state.isSavingReminder,
            onDismiss = { viewModel.onAction(ContactProfileAction.CloseAddReminder) },
            onTitleChanged = { viewModel.onAction(ContactProfileAction.ReminderDraftTitleChanged(it)) },
            onDescriptionChanged = {
                viewModel.onAction(ContactProfileAction.ReminderDraftDescriptionChanged(it))
            },
            onRecurrenceChanged = {
                viewModel.onAction(ContactProfileAction.ReminderDraftRecurrenceChanged(it))
            },
            onDateChanged = {
                viewModel.onAction(ContactProfileAction.ReminderDraftDateChanged(it))
            },
            onSave = { viewModel.onAction(ContactProfileAction.SaveReminder) },
        )
    }

    if (state.isAddNoteSheetOpen) {
        AddNoteSheet(
            draftTitle = state.draftTitle,
            draftDescription = state.draftDescription,
            isSaving = state.isSavingNote,
            onDismiss = { viewModel.onAction(ContactProfileAction.CloseAddNote) },
            onTitleChanged = { viewModel.onAction(ContactProfileAction.DraftTitleChanged(it)) },
            onDescriptionChanged = {
                viewModel.onAction(ContactProfileAction.DraftDescriptionChanged(it))
            },
            onSave = { viewModel.onAction(ContactProfileAction.SaveNote) },
        )
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
