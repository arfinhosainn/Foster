package app.usenekko.onboarding.addnote

sealed interface AddNoteEvent {
    data object NavigateToNext : AddNoteEvent
    data object NavigateBack : AddNoteEvent
    data object NavigateSkip : AddNoteEvent
    data object NavigateToHome : AddNoteEvent
    data class ShowError(val message: String) : AddNoteEvent
}
