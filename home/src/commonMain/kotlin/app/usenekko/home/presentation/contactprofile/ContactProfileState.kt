package app.usenekko.home.presentation.contactprofile

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Note

sealed interface ContactProfileAction {
    data object ToggleRelationshipInfo : ContactProfileAction
    data object CheckIn : ContactProfileAction
    data object OpenAddNote : ContactProfileAction
    data object CloseAddNote : ContactProfileAction
    data class DraftTitleChanged(val title: String) : ContactProfileAction
    data class DraftDescriptionChanged(val description: String) : ContactProfileAction
    data object SaveNote : ContactProfileAction
}

data class ContactProfileState(
    val isLoading: Boolean = true,
    val contact: Contact? = null,
    val daysUntilNextCheckIn: Int = 0,
    val isRelationshipInfoOpen: Boolean = false,
    val isCheckingIn: Boolean = false,
    val checkInError: String? = null,
    val notes: List<Note> = emptyList(),
    val isAddNoteSheetOpen: Boolean = false,
    val draftTitle: String = "",
    val draftDescription: String = "",
    val isSavingNote: Boolean = false,
    val notesError: String? = null,
)
