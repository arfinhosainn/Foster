package app.usenekko.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.GroupMembership
import app.usenekko.shared.domain.Result
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class HomeViewModel(
    private val contactDataSource: ContactDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var allContacts: List<Contact> = emptyList()
    private var memberships: List<GroupMembership> = emptyList()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val contactsResult = contactDataSource.getContacts()
            val groupsResult = contactDataSource.getGroups()
            val membershipsResult = contactDataSource.getGroupMemberships()

            when (val result = contactsResult) {
                is Result.Success -> {
                    allContacts = result.data
                    memberships = (membershipsResult as? Result.Success)?.data.orEmpty()

                    val groups = when (val g = groupsResult) {
                        is Result.Success -> g.data
                        is Result.Error -> _state.value.groups
                    }

                    val effectiveGroupId = _state.value.selectedGroupId
                        ?.takeIf { selected -> groups.any { it.id == selected } }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        groups = groups,
                        selectedGroupId = effectiveGroupId,
                    )
                    recomputeCounts(allContacts, effectiveGroupId, memberships)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.error.toString(),
                    )
                }
            }
        }
    }

    fun onGroupSelected(groupId: String?) {
        if (_state.value.selectedGroupId == groupId) return
        _state.value = _state.value.copy(selectedGroupId = groupId)
        recomputeCounts(allContacts, groupId, memberships)
    }

    private fun recomputeCounts(
        contacts: List<Contact>,
        groupId: String?,
        memberships: List<GroupMembership>,
    ) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val filtered = if (groupId == null) {
            contacts
        } else {
            val contactIds = memberships
                .filter { it.groupId == groupId }
                .mapTo(mutableSetOf()) { it.contactId }
            contacts.filter { it.id in contactIds }
        }
        _state.value = _state.value.copy(
            totalContactCount = contacts.size,
            outstandingCount = filtered.count { it.isOutstanding(today) },
            upToDateCount = filtered.count { !it.isOutstanding(today) },
        )
    }

    private fun Contact.isOutstanding(today: LocalDate): Boolean {
        val next = nextCheckInDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return true
        return next <= today
    }
}
