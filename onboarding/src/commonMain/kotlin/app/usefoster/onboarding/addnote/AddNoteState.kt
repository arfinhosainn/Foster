package app.usefoster.onboarding.addnote

data class AddNoteState(
    val notes: List<NoteItem> = emptyList(),
    val isBottomSheetVisible: Boolean = false,
    val draftTitle: String = "",
    val draftDescription: String = "",
    val isSubmitting: Boolean = false,
    val editingNoteId: String? = null,
)

data class NoteItem(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
)
