package app.usenekko.home.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupSettingsState(
    val isLoading: Boolean = true,
    val groups: List<Group> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val memberships: List<GroupMembership> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val draftName: String = "",
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val draftNames: Map<String, String> = emptyMap(),
    val error: String? = null,
) {
    fun memberCount(groupId: String): Int = memberships.count { it.groupId == groupId }
}

sealed interface GroupSettingsAction {
    data object OpenCreateDialog : GroupSettingsAction
    data object CloseCreateDialog : GroupSettingsAction
    data class DraftNameChanged(val name: String) : GroupSettingsAction
    data object CreateGroup : GroupSettingsAction
    data object StartEditing : GroupSettingsAction
    data class DraftGroupNameChanged(val groupId: String, val name: String) : GroupSettingsAction
    data object SaveChanges : GroupSettingsAction
    data object CancelEditing : GroupSettingsAction
    data class DeleteGroup(val groupId: String) : GroupSettingsAction
    data object Reload : GroupSettingsAction
}

class GroupSettingsViewModel(
    private val contactDataSource: ContactDataSource,
    private val homeRepository: HomeRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupSettingsState())
    val state: StateFlow<GroupSettingsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val contacts = contactDataSource.getContacts()
            val groups = contactDataSource.getGroups()
            val memberships = contactDataSource.getGroupMemberships()

            _state.value = GroupSettingsState(
                isLoading = false,
                contacts = (contacts as? Result.Success)?.data.orEmpty(),
                groups = (groups as? Result.Success)?.data.orEmpty(),
                memberships = (memberships as? Result.Success)?.data.orEmpty(),
                isCreateDialogOpen = _state.value.isCreateDialogOpen,
                draftName = _state.value.draftName,
                isSaving = false,
                error = (contacts as? Result.Error)?.error?.toString()
                    ?: (groups as? Result.Error)?.error?.toString()
                    ?: (memberships as? Result.Error)?.error?.toString(),
            )
        }
    }

    fun onAction(action: GroupSettingsAction) {
        when (action) {
            GroupSettingsAction.OpenCreateDialog -> {
                _state.value = _state.value.copy(isCreateDialogOpen = true, draftName = "")
            }
            GroupSettingsAction.CloseCreateDialog -> {
                _state.value = _state.value.copy(isCreateDialogOpen = false, draftName = "")
            }
            is GroupSettingsAction.DraftNameChanged -> {
                _state.value = _state.value.copy(draftName = action.name)
            }
            GroupSettingsAction.CreateGroup -> createGroup()
            GroupSettingsAction.StartEditing -> {
                _state.value = _state.value.copy(
                    isEditing = true,
                    draftNames = _state.value.groups.associate { it.id to it.name },
                    error = null,
                )
            }
            is GroupSettingsAction.DraftGroupNameChanged -> {
                if (_state.value.isEditing) {
                    _state.value = _state.value.copy(
                        draftNames = _state.value.draftNames + (action.groupId to action.name),
                    )
                }
            }
            GroupSettingsAction.SaveChanges -> saveChanges()
            GroupSettingsAction.CancelEditing -> {
                _state.value = _state.value.copy(
                    isEditing = false,
                    draftNames = emptyMap(),
                    error = null,
                )
            }
            is GroupSettingsAction.DeleteGroup -> deleteGroup(action.groupId)
            GroupSettingsAction.Reload -> load()
        }
    }

    private fun createGroup() {
        val name = _state.value.draftName.trim()
        if (name.isEmpty() || _state.value.isSaving) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            when (val result = contactDataSource.createGroup(name = name, color = null)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isCreateDialogOpen = false,
                        draftName = "",
                    )
                    homeRepository?.invalidate()
                    load()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.error.toString(),
                    )
                }
            }
        }
    }

    private fun saveChanges() {
        if (!_state.value.isEditing || _state.value.isSaving) return

        val changes = _state.value.groups.mapNotNull { group ->
            val name = _state.value.draftNames[group.id]?.trim().orEmpty()
            if (name.isNotEmpty() && name != group.name) group.id to name else null
        }

        if (changes.isEmpty()) {
            _state.value = _state.value.copy(isEditing = false, draftNames = emptyMap())
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            for ((groupId, name) in changes) {
                when (val result = contactDataSource.updateGroup(groupId, name)) {
                    is Result.Success -> Unit
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.error.toString(),
                        )
                        return@launch
                    }
                }
            }
            _state.value = _state.value.copy(
                isSaving = false,
                isEditing = false,
                draftNames = emptyMap(),
            )
            homeRepository?.invalidate()
            load()
        }
    }

    private fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            when (val result = contactDataSource.deleteGroup(groupId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(draftNames = _state.value.draftNames - groupId)
                    homeRepository?.invalidate()
                    load()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(error = result.error.toString())
                }
            }
        }
    }
}