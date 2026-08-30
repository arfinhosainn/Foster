package app.usefoster.onboarding.addnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usefoster.onboarding.domain.NoteDraft
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AddNoteViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        AddNoteState(notes = draftStore.draft.value.notes.map { it.toNoteItem() })
    )
    val state = _state.asStateFlow()

    private val _events = Channel<AddNoteEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.AddNote) }
        viewModelScope.launch {
            draftStore.draft.collect { draft ->
                _state.update { it.copy(notes = draft.notes.map { note -> note.toNoteItem() }) }
            }
        }
    }

    fun onAction(action: AddNoteAction) {
        when (action) {
            is AddNoteAction.AddClicked -> {
                _state.update {
                    it.copy(
                        isBottomSheetVisible = true,
                        draftTitle = "",
                        draftDescription = "",
                        editingNoteId = null,
                    )
                }
            }
            is AddNoteAction.EditClicked -> {
                val note = draftStore.draft.value.notes
                    .firstOrNull { it.id == action.noteId }
                    ?: return
                _state.update {
                    it.copy(
                        isBottomSheetVisible = true,
                        draftTitle = note.title,
                        draftDescription = note.body,
                        editingNoteId = note.id,
                    )
                }
            }
            is AddNoteAction.BottomSheetDismissed -> {
                _state.update {
                    it.copy(
                        isBottomSheetVisible = false,
                        draftTitle = "",
                        draftDescription = "",
                        editingNoteId = null,
                    )
                }
            }
            is AddNoteAction.DraftTitleChanged -> {
                _state.update { it.copy(draftTitle = action.title) }
            }
            is AddNoteAction.DraftDescriptionChanged -> {
                _state.update { it.copy(draftDescription = action.description) }
            }
            is AddNoteAction.SaveClicked -> {
                val state = _state.value
                val draft = draftStore.draft.value
                val newNote = NoteDraft(
                    id = state.editingNoteId ?: "note_${draft.notes.size}",
                    title = state.draftTitle.ifEmpty { "Untitled" },
                    body = state.draftDescription,
                )
                draftStore.update {
                    it.copy(
                        notes = if (state.editingNoteId == null) {
                            it.notes + newNote
                        } else {
                            it.notes.map { note ->
                                if (note.id == state.editingNoteId) newNote else note
                            }
                        },
                        currentStep = OnboardingStep.AddNote,
                    )
                }
                _state.update {
                    it.copy(
                        isBottomSheetVisible = false,
                        draftTitle = "",
                        draftDescription = "",
                        editingNoteId = null,
                    )
                }
            }
            is AddNoteAction.DeleteNote -> {
                draftStore.update {
                    it.copy(notes = it.notes.filter { note -> note.id != action.id })
                }
                _state.update {
                    it.copy(notes = it.notes.filter { note -> note.id != action.id })
                }
            }
        }
    }

    fun onNavigateToNext() {
        draftStore.update {
            it.copy(currentStep = OnboardingStep.Notification)
        }
        viewModelScope.launch { _events.send(AddNoteEvent.NavigateToNext) }
    }

    fun onSkip() {
        draftStore.update {
            it.copy(currentStep = OnboardingStep.Notification)
        }
        viewModelScope.launch { _events.send(AddNoteEvent.NavigateSkip) }
    }

    fun onBack() {
        viewModelScope.launch { _events.send(AddNoteEvent.NavigateBack) }
    }
}

private fun NoteDraft.toNoteItem(): NoteItem {
    val now = Clock.System.now()
    val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dayOfWeek = localDate.dayOfWeek.name.take(3)
    val day = localDate.day.toString().padStart(2, '0')
    val month = localDate.month.name.take(3)
    val year = (localDate.year % 100).toString().padStart(2, '0')
    return NoteItem(
        id = id,
        title = title,
        description = body,
        date = "$dayOfWeek, $day $month $year",
    )
}
