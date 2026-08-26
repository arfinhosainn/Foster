package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.usenekko.home.di.rememberContactProfileViewModel
import app.usenekko.home.di.rememberEditContactViewModel
import app.usenekko.home.addcontact.EditContactSheet
import app.usenekko.home.domain.checkInProgressFraction
import app.usenekko.theme.NekkoTheme
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun ContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    onBrainstormClick: () -> Unit,
    isSupportingPane: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberContactProfileViewModel(contactId)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showEditContact by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refreshIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Contact no longer exists (or the lookup failed) — pop back to Home instead
    // of rendering a broken screen.
    val notFound = !state.isLoading && state.contact == null
    LaunchedEffect(notFound) {
        if (notFound) onBack()
    }

    Scaffold(
        modifier = modifier.then(
            if (isSupportingPane) Modifier.padding(horizontal = 8.dp) else Modifier,
        ),
        containerColor = NekkoTheme.colors.background.b0,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isSupportingPane) 16.dp else 24.dp),
            ) {
                if (state.isRefreshing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 52.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = NekkoTheme.colors.text.tertiary,
                            strokeWidth = 1.5.dp,
                        )
                    }
                }
                if (state.isLoading && state.contact == null) {
                    ContactProfileLoadingSkeleton(
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    state.contact?.let { c ->
                        val today = remember(c.nextCheckInDate, c.checkInFrequency) {
                            Clock.System.todayIn(TimeZone.currentSystemDefault())
                        }
                        ContactProfileHeader(
                            name = c.name,
                            avatarColor = c.avatarColor,
                            frequencyLabel = formatContactFrequencyLabel(c.checkInFrequency),
                            reminderTime = formatContactReminderTime(c.reminderTime),
                            isExpanded = state.isRelationshipInfoOpen,
                            daysUntilNextCheckIn = state.daysUntilNextCheckIn,
                            ringProgress = c.checkInProgressFraction(today),
                            onNameClick = {
                                viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo)
                            },
                            onNotificationClick = {
                                viewModel.onAction(ContactProfileAction.OpenReminderList)
                            },
                            onCheckInClick = { viewModel.onAction(ContactProfileAction.CheckIn) },
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    BrainstormCard(
                        onClick = onBrainstormClick,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    NotesSection(
                        notes = state.notes,
                        onAddNote = { viewModel.onAction(ContactProfileAction.OpenAddNote) },
                        onDeleteNote = { viewModel.onAction(ContactProfileAction.DeleteNote(it)) },
                    )
                }
            }

            ContactProfileActionBar(
                onBack = onBack,
                onEditClick = { showEditContact = true },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (state.isRelationshipInfoOpen) {
        RelationshipInfoSheet(
            contactName = state.contact?.name.orEmpty(),
            avatarColor = state.contact?.avatarColor,
            userSelectedAvatarId = state.userSelectedAvatarId,
            checkInCount = state.checkInCount,
            nextCheckInDate = state.contact?.nextCheckInDate,
            onDismiss = { viewModel.onAction(ContactProfileAction.ToggleRelationshipInfo) },
        )
    }

    if (state.isReminderListSheetOpen) {
        ReminderBottomSheet(
            reminders = state.reminders,
            onDismiss = { viewModel.onAction(ContactProfileAction.CloseReminderList) },
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
                draftTimeOfDay = state.reminderDraftTimeOfDay,
                isSaving = state.isSavingReminder,
                isEditing = state.editingReminderId != null,
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
            onTimeChanged = {
                viewModel.onAction(ContactProfileAction.ReminderDraftTimeChanged(it))
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

    if (showEditContact) {
        state.contact?.let { contact ->
            val editViewModel = rememberEditContactViewModel(contact)
            EditContactSheet(
                viewModel = editViewModel,
                onDismiss = { showEditContact = false },
                onSaved = {
                    showEditContact = false
                    viewModel.refreshIfStale(forceRefresh = true)
                },
            )
        }
    }
}
