package app.usefoster.onboarding.addnote

sealed interface AddNoteEvent {
    data object NavigateToNext : AddNoteEvent
    data object NavigateBack : AddNoteEvent
    data object NavigateSkip : AddNoteEvent
    data class ShowError(val message: String) : AddNoteEvent
}
