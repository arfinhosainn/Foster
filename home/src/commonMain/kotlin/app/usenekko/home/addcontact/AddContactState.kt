package app.usenekko.home.addcontact

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership

data class AddContactState(
    val currentStep: Int = 0,
    val name: String = "",
    val selectedAvatarIndex: Int? = null,
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val memberships: List<GroupMembership> = emptyList(),
    val groupsLoading: Boolean = false,
    val selectedGroupId: String? = null,
    val showCreateGroupSheet: Boolean = false,
    val isCreatingGroup: Boolean = false,
    val selectedFrequency: String = "weekly",
    val selectedHour: Int = 10,
    val selectedMinute: Int = 30,
    val isAm: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
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
