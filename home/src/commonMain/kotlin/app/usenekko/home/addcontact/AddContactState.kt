package app.usenekko.home.addcontact

import app.usenekko.home.domain.Group

data class AddContactState(
    val currentStep: Int = 0,
    val name: String = "",
    val selectedColorIndex: Int? = null,
    val groups: List<Group> = emptyList(),
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
        get() = when (currentStep) {
            0 -> name.isNotBlank() && selectedColorIndex != null
            else -> true
        }

    val canSubmit: Boolean
        get() = name.isNotBlank() && selectedColorIndex != null && !isSubmitting
}
