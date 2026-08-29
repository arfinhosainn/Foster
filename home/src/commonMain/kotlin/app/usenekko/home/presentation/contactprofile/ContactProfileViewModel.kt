package app.usenekko.home.presentation.contactprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.ContactProfileRepository
import app.usenekko.home.data.ContactProfileRepositoryState
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.computeCheckInUpdate
import app.usenekko.home.domain.isOutstanding
import app.usenekko.home.domain.isCheckedInToday
import app.usenekko.home.domain.nextCheckInDateLocal
import app.usenekko.home.domain.toUserMessageResource
import app.usenekko.home.presentation.badges.detectAndTriggerBadgeReveal
import app.usenekko.home.presentation.badges.unlockedBadgeIdsOrNull
import app.usenekko.shared.domain.Result
import app.usenekko.shared.domain.ProfileDataSource
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class ContactProfileViewModel(
    private val contactId: String,
    private val contactDataSource: ContactDataSource,
    private val profileDataSource: ProfileDataSource? = null,
    private val contactProfileRepository: ContactProfileRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactProfileState())
    val state: StateFlow<ContactProfileState> = _state.asStateFlow()

    init {
        if (contactProfileRepository != null) observeProfileRepository()
        loadContact()
    }

    private fun observeProfileRepository() {
        viewModelScope.launch {
            contactProfileRepository?.state?.collectLatest { repositoryState ->
                applyRepositoryState(repositoryState)
            }
        }
    }

    private fun applyRepositoryState(repositoryState: ContactProfileRepositoryState) {
        val snapshot = repositoryState.snapshots[contactId]
        val error = repositoryState.errors[contactId]
        if (snapshot != null) {
            val contact = snapshot.contact
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = contactId in repositoryState.refreshingContactIds,
                contact = contact,
                notes = snapshot.notes,
                reminders = snapshot.reminders,
                checkInCount = snapshot.checkInCount,
                daysUntilNextCheckIn = contact?.let { daysUntilNextCheckIn(it) } ?: 0,
                refreshError = error?.toUserMessageResource(),
            )
        } else if (error != null) {
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                refreshError = error.toUserMessageResource(),
            )
        }
    }

    fun refreshIfStale(forceRefresh: Boolean = false) {
        loadContact(forceRefresh)
    }

    private fun loadContact(forceRefresh: Boolean = false) {
        if (contactProfileRepository != null) {
            viewModelScope.launch {
                contactProfileRepository.load(contactId, forceRefresh)
            }
            return
        }

        viewModelScope.launch {
            // Reuses the already-fetched contacts list rather than adding a new
            // single-row query. Missing contact => gracefully pop back to Home.
            val result = contactDataSource.getContacts()
            val contact = (result as? Result.Success)?.data?.firstOrNull { it.id == contactId }
            _state.value = _state.value.copy(
                isLoading = false,
                contact = contact,
                daysUntilNextCheckIn = contact?.let { daysUntilNextCheckIn(it) } ?: 0,
            )
        }
        loadNotes()
        loadReminders()
        loadCheckInStats()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        profileDataSource ?: return
        viewModelScope.launch {
            val result = profileDataSource.getProfile()
            val profile = (result as? Result.Success)?.data
            _state.value = _state.value.copy(userSelectedAvatarId = profile?.selectedAvatarId)
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            when (val result = contactDataSource.getNotes(contactId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(notes = result.data, notesError = null)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(notesError = result.error.toUserMessageResource())
                }
            }
        }
    }

    private fun loadReminders() {
        viewModelScope.launch {
            when (val result = contactDataSource.getReminders(contactId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(reminders = result.data, remindersError = null)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(remindersError = result.error.toUserMessageResource())
                }
            }
        }
    }

    private fun loadCheckInStats() {
        viewModelScope.launch {
            // Total check-in count derived from existing rows — no extra column.
            val result = contactDataSource.getCheckIns(contactId, "1970-01-01", "2999-12-31")
            val count = (result as? Result.Success)?.data?.size ?: 0
            _state.value = _state.value.copy(checkInCount = count)
        }
    }

    fun onAction(action: ContactProfileAction) {
        when (action) {
            ContactProfileAction.ToggleRelationshipInfo -> {
                _state.value = _state.value.copy(
                    isRelationshipInfoOpen = !_state.value.isRelationshipInfoOpen,
                )
            }
            ContactProfileAction.OpenReminderList -> {
                _state.value = _state.value.copy(isReminderListSheetOpen = true)
            }
            ContactProfileAction.CloseReminderList -> {
                _state.value = _state.value.copy(isReminderListSheetOpen = false)
            }
            ContactProfileAction.CheckIn -> checkIn()
            ContactProfileAction.OpenAddNote -> {
                _state.value = _state.value.copy(isAddNoteSheetOpen = true)
            }
            ContactProfileAction.CloseAddNote -> {
                _state.value = _state.value.copy(isAddNoteSheetOpen = false)
            }
            is ContactProfileAction.DraftTitleChanged -> {
                _state.value = _state.value.copy(draftTitle = action.title)
            }
            is ContactProfileAction.DraftDescriptionChanged -> {
                _state.value = _state.value.copy(draftDescription = action.description)
            }
            ContactProfileAction.SaveNote -> saveNote()
            is ContactProfileAction.DeleteNote -> deleteNote(action.noteId)
            ContactProfileAction.OpenAddReminder -> {
                _state.value = _state.value.copy(
                    isReminderListSheetOpen = false,
                    isAddReminderSheetOpen = true,
                    editingReminderId = null,
                    reminderDraftTitle = "",
                    reminderDraftDescription = "",
                    reminderDraftRecurrence = "None",
                    reminderDraftDateEpochMillis = null,
                    reminderDraftTimeOfDay = null,
                )
            }
            ContactProfileAction.CloseAddReminder -> {
                _state.value = _state.value.copy(
                    isAddReminderSheetOpen = false,
                    editingReminderId = null,
                )
            }
            is ContactProfileAction.ReminderDraftTitleChanged -> {
                _state.value = _state.value.copy(reminderDraftTitle = action.title)
            }
            is ContactProfileAction.ReminderDraftDescriptionChanged -> {
                _state.value = _state.value.copy(reminderDraftDescription = action.description)
            }
            is ContactProfileAction.ReminderDraftRecurrenceChanged -> {
                _state.value = _state.value.copy(reminderDraftRecurrence = action.recurrence)
            }
            is ContactProfileAction.ReminderDraftDateChanged -> {
                _state.value = _state.value.copy(reminderDraftDateEpochMillis = action.dateEpochMillis)
            }
            is ContactProfileAction.ReminderDraftTimeChanged -> {
                _state.value = _state.value.copy(reminderDraftTimeOfDay = action.timeOfDay)
            }
            ContactProfileAction.SaveReminder -> saveReminder()
            is ContactProfileAction.EditReminder -> editReminder(action.reminderId)
            is ContactProfileAction.DeleteReminder -> deleteReminder(action.reminderId)
        }
    }

    private fun saveNote() {
        if (_state.value.isSavingNote) return
        val title = _state.value.draftTitle.trim()
        if (title.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingNote = true, notesError = null)
            val result = contactDataSource.createNote(
                contactId = contactId,
                title = title,
                body = _state.value.draftDescription.trim(),
            )

            when (result) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isSavingNote = false,
                        isAddNoteSheetOpen = false,
                        draftTitle = "",
                        draftDescription = "",
                    )
                    refreshProfileAfterMutation()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isSavingNote = false,
                        notesError = result.error.toUserMessageResource(),
                    )
                }
            }
        }
    }

    private fun checkIn() {
        val current = _state.value.contact ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        // Same-day idempotency: only outstanding contacts can check in, matching
        // Home's list. After a successful check-in nextCheckInDate moves into the
        // future, so a second tap on the same day is a no-op.
        if (_state.value.isCheckingIn || !current.isOutstanding(today) || current.isCheckedInToday(today)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingIn = true, checkInError = null)

            val previousBadges = contactDataSource.unlockedBadgeIdsOrNull()

            val update = computeCheckInUpdate(current, today)
            val result = contactDataSource.logCheckIn(
                contactId = current.id,
                lastCheckInDate = update.lastCheckInDate,
                nextCheckInDate = update.nextCheckInDate,
                streakCount = update.streakCount,
                checkedInAt = Clock.System.now().toString(),
            )

            when (result) {
                is Result.Success -> {
                    val updated = result.data
                    _state.value = _state.value.copy(
                        isCheckingIn = false,
                        contact = updated,
                        daysUntilNextCheckIn = daysUntilNextCheckIn(updated),
                    )
                    refreshProfileAfterMutation()
                    if (previousBadges != null) {
                        contactDataSource.detectAndTriggerBadgeReveal(previousBadges)
                    }
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isCheckingIn = false,
                        checkInError = result.error.toUserMessageResource(),
                    )
                }
            }
        }
    }

    private fun deleteNote(noteId: String) {
        viewModelScope.launch {
            when (val result = contactDataSource.deleteNote(noteId)) {
                is Result.Success -> refreshProfileAfterMutation()
                is Result.Error -> {
                    _state.value = _state.value.copy(notesError = result.error.toUserMessageResource())
                }
            }
        }
    }

    private fun saveReminder() {
        if (_state.value.isSavingReminder) return
        val title = _state.value.reminderDraftTitle.trim()
        if (title.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingReminder = true, remindersError = null)
            val editingId = _state.value.editingReminderId
            val result = if (editingId != null) {
                contactDataSource.updateReminder(
                    reminderId = editingId,
                    title = title,
                    description = _state.value.reminderDraftDescription.trim(),
                    recurrence = _state.value.reminderDraftRecurrence,
                    date = _state.value.reminderDraftDateEpochMillis,
                    timeOfDay = _state.value.reminderDraftTimeOfDay,
                )
            } else {
                contactDataSource.createReminder(
                    contactId = contactId,
                    title = title,
                    description = _state.value.reminderDraftDescription.trim(),
                    recurrence = _state.value.reminderDraftRecurrence,
                    date = _state.value.reminderDraftDateEpochMillis,
                    timeOfDay = _state.value.reminderDraftTimeOfDay,
                )
            }

            when (result) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isSavingReminder = false,
                        isAddReminderSheetOpen = false,
                        isReminderListSheetOpen = true,
                        editingReminderId = null,
                        reminderDraftTitle = "",
                        reminderDraftDescription = "",
                        reminderDraftRecurrence = "none",
                        reminderDraftDateEpochMillis = null,
                        reminderDraftTimeOfDay = null,
                    )
                    refreshProfileAfterMutation()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isSavingReminder = false,
                        remindersError = result.error.toUserMessageResource(),
                    )
                }
            }
        }
    }

    private fun editReminder(reminderId: String) {
        // Opens the same add-reminder form pre-filled with the existing values.
        val reminder = _state.value.reminders.firstOrNull { it.id == reminderId } ?: return
        _state.value = _state.value.copy(
            reminderDraftTitle = reminder.title,
            reminderDraftDescription = reminder.description,
            reminderDraftRecurrence = reminder.recurrence,
            reminderDraftDateEpochMillis = reminder.dateEpochMillis,
            reminderDraftTimeOfDay = reminder.timeOfDay,
            editingReminderId = reminder.id,
            isReminderListSheetOpen = false,
            isAddReminderSheetOpen = true,
        )
    }

    private fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            when (val result = contactDataSource.deleteReminder(reminderId)) {
                is Result.Success -> refreshProfileAfterMutation()
                is Result.Error -> {
                    _state.value = _state.value.copy(remindersError = result.error.toUserMessageResource())
                }
            }
        }
    }

    private fun refreshProfileAfterMutation() {
        contactProfileRepository?.invalidate(contactId)
        loadContact(forceRefresh = contactProfileRepository != null)
    }

    private fun daysUntilNextCheckIn(contact: Contact): Int {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val next = contact.nextCheckInDateLocal() ?: return 0
        return (next.toEpochDays() - today.toEpochDays()).toInt().coerceAtLeast(0)
    }
}
