package app.usenekko.onboarding.addnote

sealed interface AddNoteAction {
    data object AddClicked : AddNoteAction
    data class EditClicked(val noteId: String) : AddNoteAction
    data object BottomSheetDismissed : AddNoteAction
    data class DraftTitleChanged(val title: String) : AddNoteAction
    data class DraftDescriptionChanged(val description: String) : AddNoteAction
    data object SaveClicked : AddNoteAction
    data class DeleteNote(val id: String) : AddNoteAction
}
