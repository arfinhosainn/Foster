package app.usenekko.onboarding.addnote

sealed interface AddNoteEvent {
    data object NavigateToNext : AddNoteEvent
    data object NavigateBack : AddNoteEvent
    data object NavigateSkip : AddNoteEvent
}
