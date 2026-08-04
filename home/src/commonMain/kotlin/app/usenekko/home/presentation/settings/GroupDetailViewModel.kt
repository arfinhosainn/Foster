package app.usenekko.home.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupDetailState(
    val groupId: String = "",
    val groupName: String = "",
    val isLoading: Boolean = true,
    val members: List<Contact> = emptyList(),
    val otherGroups: List<Group> = emptyList(),
    val isMoveDialogOpen: Boolean = false,
    val movingContact: Contact? = null,
    val isMutating: Boolean = false,
    val error: String? = null,
)

sealed interface GroupDetailAction {
    data class OpenMoveDialog(val contact: Contact) : GroupDetailAction
    data object CloseMoveDialog : GroupDetailAction
    data class MoveMember(val contactId: String, val toGroupId: String) : GroupDetailAction
    data class RemoveMember(val contactId: String) : GroupDetailAction
    data object Reload : GroupDetailAction
}

class GroupDetailViewModel(
    private val groupId: String,
    private val contactDataSource: ContactDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupDetailState(groupId = groupId))
    val state: StateFlow<GroupDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val contactsResult = contactDataSource.getContacts()
            val groupsResult = contactDataSource.getGroups()
            val membershipsResult = contactDataSource.getGroupMemberships()

            val allContacts = (contactsResult as? Result.Success)?.data.orEmpty()
            val groups = (groupsResult as? Result.Success)?.data.orEmpty()
            val memberships = (membershipsResult as? Result.Success)?.data.orEmpty()

            val memberIds = memberships
                .filter { it.groupId == groupId }
                .mapTo(mutableSetOf()) { it.contactId }
            val members = allContacts.filter { it.id in memberIds }
            val groupName = groups.firstOrNull { it.id == groupId }?.name.orEmpty()

            _state.value = _state.value.copy(
                isLoading = false,
                groupName = groupName,
                members = members,
                otherGroups = groups.filter { it.id != groupId },
                error = (contactsResult as? Result.Error)?.error?.toString()
                    ?: (groupsResult as? Result.Error)?.error?.toString()
                    ?: (membershipsResult as? Result.Error)?.error?.toString(),
            )
        }
    }

    fun onAction(action: GroupDetailAction) {
        when (action) {
            is GroupDetailAction.OpenMoveDialog -> {
                _state.value = _state.value.copy(
                    isMoveDialogOpen = true,
                    movingContact = action.contact,
                )
            }
            GroupDetailAction.CloseMoveDialog -> {
                _state.value = _state.value.copy(isMoveDialogOpen = false, movingContact = null)
            }
            is GroupDetailAction.MoveMember -> moveMember(action.contactId, action.toGroupId)
            is GroupDetailAction.RemoveMember -> removeMember(action.contactId)
            GroupDetailAction.Reload -> load()
        }
    }

    private fun moveMember(contactId: String, toGroupId: String) {
        if (_state.value.isMutating || toGroupId == groupId) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, error = null)
            when (
                val result = contactDataSource.moveContactToGroup(contactId, groupId, toGroupId)
            ) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isMutating = false,
                        isMoveDialogOpen = false,
                        movingContact = null,
                    )
                    load()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isMutating = false,
                        error = result.error.toString(),
                    )
                }
            }
        }
    }

    private fun removeMember(contactId: String) {
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, error = null)
            when (val result = contactDataSource.removeContactFromGroup(contactId, groupId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(isMutating = false)
                    load()
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isMutating = false,
                        error = result.error.toString(),
                    )
                }
            }
        }
    }
}