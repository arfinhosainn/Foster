package app.usenekko.home.presentation.contactprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.computeCheckInUpdate
import app.usenekko.home.domain.isOutstanding
import app.usenekko.home.domain.nextCheckInDateLocal
import app.usenekko.shared.domain.Result
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class ContactProfileViewModel(
    private val contactId: String,
    private val contactDataSource: ContactDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactProfileState())
    val state: StateFlow<ContactProfileState> = _state.asStateFlow()

    init {
        loadContact()
    }

    private fun loadContact() {
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
    }

    private fun loadNotes() {
        viewModelScope.launch {
            when (val result = contactDataSource.getNotes(contactId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(notes = result.data, notesError = null)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(notesError = result.error.toString())
                }
            }
        }
    }

    fun onAction(action: ContactProfileAction) {
        when (action) {
            ContactProfileAction.ToggleRelationshipInfo -> {
                _state.value = _state.value.copy(
                    isRelationshipInfoOpen = !_state.value.isRelationshipInfoOpen,
                )
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
                    loadNotes()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isSavingNote = false,
                        notesError = result.error.toString(),
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
        if (_state.value.isCheckingIn || !current.isOutstanding(today)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingIn = true, checkInError = null)

            val update = computeCheckInUpdate(current, today)
            val result = contactDataSource.logCheckIn(
                contactId = current.id,
                lastCheckInDate = update.lastCheckInDate,
                nextCheckInDate = update.nextCheckInDate,
                streakCount = update.streakCount,
            )

            when (result) {
                is Result.Success -> {
                    val updated = result.data
                    _state.value = _state.value.copy(
                        isCheckingIn = false,
                        contact = updated,
                        daysUntilNextCheckIn = daysUntilNextCheckIn(updated),
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isCheckingIn = false,
                        checkInError = result.error.toString(),
                    )
                }
            }
        }
    }

    private fun daysUntilNextCheckIn(contact: Contact): Int {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val next = contact.nextCheckInDateLocal() ?: return 0
        return (next.toEpochDays() - today.toEpochDays()).toInt().coerceAtLeast(0)
    }
}
