package app.usenekko.home.presentation.contactprofile

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Note
import app.usenekko.home.domain.Reminder

sealed interface ContactProfileAction {
    data object ToggleRelationshipInfo : ContactProfileAction
    data object OpenReminderList : ContactProfileAction
    data object CloseReminderList : ContactProfileAction
    data object CheckIn : ContactProfileAction
    data object OpenAddNote : ContactProfileAction
    data object CloseAddNote : ContactProfileAction
    data class DraftTitleChanged(val title: String) : ContactProfileAction
    data class DraftDescriptionChanged(val description: String) : ContactProfileAction
    data object SaveNote : ContactProfileAction
    data class DeleteNote(val noteId: String) : ContactProfileAction
    data object OpenAddReminder : ContactProfileAction
    data object CloseAddReminder : ContactProfileAction
    data class ReminderDraftTitleChanged(val title: String) : ContactProfileAction
    data class ReminderDraftDescriptionChanged(val description: String) : ContactProfileAction
    data class ReminderDraftRecurrenceChanged(val recurrence: String) : ContactProfileAction
    data class ReminderDraftDateChanged(val dateEpochMillis: Long?) : ContactProfileAction
    data object SaveReminder : ContactProfileAction
    data class EditReminder(val reminderId: String) : ContactProfileAction
    data class DeleteReminder(val reminderId: String) : ContactProfileAction
}

data class ContactProfileState(
    val isLoading: Boolean = true,
    val contact: Contact? = null,
    val userSelectedAvatarId: String? = null,
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
    val reminders: List<Reminder> = emptyList(),
    val checkInCount: Int = 0,
    val isReminderListSheetOpen: Boolean = false,
    val isAddReminderSheetOpen: Boolean = false,
    val reminderDraftTitle: String = "",
    val reminderDraftDescription: String = "",
    val reminderDraftRecurrence: String = "None",
    val reminderDraftDateEpochMillis: Long? = null,
    val isSavingReminder: Boolean = false,
    val remindersError: String? = null,
)
