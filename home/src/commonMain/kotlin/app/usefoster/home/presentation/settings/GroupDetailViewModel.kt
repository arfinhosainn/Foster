package app.usefoster.home.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usefoster.home.data.HomeRepository
import app.usefoster.home.data.HomeRepositoryState
import app.usefoster.home.data.HomeSnapshot
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactDataSource
import app.usefoster.home.domain.Group
import app.usefoster.home.domain.GroupMembership
import app.usefoster.home.domain.toUserMessageResource
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

data class GroupDetailState(
    val groupId: String = "",
    val groupName: String = "",
    val isLoading: Boolean = true,
    val members: List<Contact> = emptyList(),
    val checkInCounts: Map<String, Int> = emptyMap(),
    val otherGroups: List<Group> = emptyList(),
    val isRefreshing: Boolean = false,
    val isMoveDialogOpen: Boolean = false,
    val movingContact: Contact? = null,
    val isMutating: Boolean = false,
    val error: StringResource? = null,
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
    private val homeRepository: HomeRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupDetailState(groupId = groupId))
    val state: StateFlow<GroupDetailState> = _state.asStateFlow()

    init {
        if (homeRepository != null) observeHomeRepository()
        load()
    }

    private fun observeHomeRepository() {
        viewModelScope.launch {
            homeRepository?.state?.collectLatest { repositoryState ->
                applyRepositoryState(repositoryState)
            }
        }
    }

    private fun applyRepositoryState(repositoryState: HomeRepositoryState) {
        val snapshot = repositoryState.snapshot
        if (snapshot != null) {
            applySnapshot(snapshot, repositoryState)
        } else if (repositoryState.error != null) {
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = repositoryState.error.toUserMessageResource(),
            )
        } else if (repositoryState.isRefreshing) {
            _state.value = _state.value.copy(isLoading = true, isRefreshing = true)
        }
    }

    private fun applySnapshot(snapshot: HomeSnapshot, repositoryState: HomeRepositoryState) {
        val memberIds = snapshot.memberships
            .filter { it.groupId == groupId }
            .mapTo(mutableSetOf()) { it.contactId }
        val members = snapshot.contacts.filter { it.id in memberIds }
        val checkInCounts = snapshot.checkInHistory
            .groupingBy { it.contactId }
            .eachCount()

        _state.value = _state.value.copy(
            isLoading = false,
            isRefreshing = repositoryState.isRefreshing,
            groupName = snapshot.groups.firstOrNull { it.id == groupId }?.name.orEmpty(),
            members = members,
            checkInCounts = checkInCounts,
            otherGroups = snapshot.groups.filter { it.id != groupId },
            error = repositoryState.error?.toUserMessageResource(),
        )
    }

    fun refreshIfStale() {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            loadInternal(forceRefresh)
        }
    }

    private suspend fun loadInternal(forceRefresh: Boolean = false) {
        if (homeRepository != null) {
            homeRepository.load(forceRefresh)
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)

        val contactsResult = contactDataSource.getContacts()
        val groupsResult = contactDataSource.getGroups()
        val membershipsResult = contactDataSource.getGroupMemberships()
        val checkInsResult = contactDataSource.getCheckIns(null, "1970-01-01", "2999-12-31")

        val allContacts = (contactsResult as? Result.Success)?.data.orEmpty()
        val groups = (groupsResult as? Result.Success)?.data.orEmpty()
        val memberships = (membershipsResult as? Result.Success)?.data.orEmpty()
        val checkInCounts = (checkInsResult as? Result.Success)
            ?.data
            ?.groupingBy { it.contactId }
            ?.eachCount()
            .orEmpty()

        val memberIds = memberships
            .filter { it.groupId == groupId }
            .mapTo(mutableSetOf()) { it.contactId }
        val members = allContacts.filter { it.id in memberIds }
        val groupName = groups.firstOrNull { it.id == groupId }?.name.orEmpty()

        _state.value = _state.value.copy(
            isLoading = false,
            groupName = groupName,
            members = members,
            checkInCounts = checkInCounts,
            otherGroups = groups.filter { it.id != groupId },
            error = (contactsResult as? Result.Error)?.error?.toUserMessageResource()
                ?: (groupsResult as? Result.Error)?.error?.toUserMessageResource()
                ?: (membershipsResult as? Result.Error)?.error?.toUserMessageResource()
                ?: (checkInsResult as? Result.Error)?.error?.toUserMessageResource(),
        )
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
                        isMoveDialogOpen = false,
                        movingContact = null,
                    )
                    homeRepository?.invalidate()
                    loadInternal(forceRefresh = true)
                    _state.value = _state.value.copy(isMutating = false)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isMutating = false,
                        error = result.error.toUserMessageResource(),
                    )
                }
            }
        }
    }

    private fun removeMember(contactId: String) {
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, error = null)
            when (val result = contactDataSource.deleteContact(contactId)) {
                is Result.Success -> {
                    homeRepository?.invalidate()
                    loadInternal(forceRefresh = true)
                    _state.value = _state.value.copy(isMutating = false)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isMutating = false,
                        error = result.error.toUserMessageResource(),
                    )
                }
            }
        }
    }
}