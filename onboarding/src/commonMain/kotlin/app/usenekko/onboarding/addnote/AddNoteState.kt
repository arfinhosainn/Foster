package app.usenekko.onboarding.addnote

data class AddNoteState(
    val notes: List<NoteItem> = emptyList(),
    val isBottomSheetVisible: Boolean = false,
    val draftTitle: String = "",
    val draftDescription: String = "",
    val isSubmitting: Boolean = false,
)

data class NoteItem(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
)
