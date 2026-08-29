package app.usenekko.home.addcontact

import org.jetbrains.compose.resources.StringResource

import androidx.compose.ui.graphics.ImageBitmap
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.contacts.ImportedContact

data class AddContactState(
    val currentStep: Int = 0,
    val editingContactId: String? = null,
    val name: String = "",
    val initialName: String = "",
    val selectedAvatarIndex: Int? = null,
    val initialAvatarIndex: Int? = null,
    val importedPhoto: ImageBitmap? = null,
    val importedPhoneNumber: String? = null,
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val memberships: List<GroupMembership> = emptyList(),
    val groupsLoading: Boolean = false,
    val selectedGroupId: String? = null,
    val initialGroupId: String? = null,
    val initialGroupResolved: Boolean = true,
    val showCreateGroupSheet: Boolean = false,
    val isCreatingGroup: Boolean = false,
    val selectedFrequency: String = "weekly",
    val initialFrequency: String = "weekly",
    val selectedHour: Int = 10,
    val selectedMinute: Int = 30,
    val isAm: Boolean = false,
    val initialHour: Int = 10,
    val initialMinute: Int = 30,
    val initialIsAm: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: StringResource? = null,
) {
    val isEditing: Boolean
        get() = editingContactId != null

    val hasChanges: Boolean
        get() = isEditing && initialGroupResolved && (
            name.trim() != initialName.trim() ||
                selectedAvatarIndex != initialAvatarIndex ||
                selectedGroupId != initialGroupId ||
                selectedFrequency != initialFrequency ||
                selectedHour != initialHour ||
                selectedMinute != initialMinute ||
                isAm != initialIsAm
            )

    val canSaveChanges: Boolean
        get() = isEditing && hasChanges && !isSubmitting

    val canAdvanceFromStep: Boolean
        get() = true

    val canSubmit: Boolean
        get() = !isSubmitting
}

fun AddContactState.withTimeDialValue(totalMinutes: Int): AddContactState {
    val safeTotalMinutes = totalMinutes.coerceIn(0, 12 * 60 - 1)
    val hour = safeTotalMinutes / 60
    return copy(
        selectedHour = if (hour == 0) 12 else hour,
        selectedMinute = safeTotalMinutes % 60,
    )
}

fun AddContactState.withImportedContact(contact: ImportedContact): AddContactState {
    if (contact.name.isBlank()) return this

    return copy(
        name = contact.name,
        selectedAvatarIndex = if (contact.photo != null) null else selectedAvatarIndex,
        importedPhoto = contact.photo,
        importedPhoneNumber = contact.phoneNumber,
        error = null,
    )
}
