package app.usenekko.home.addcontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.data.HomeGroupPickerState
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.domain.initialReminder
import app.usenekko.home.domain.nextReminder
import app.usenekko.shared.domain.Result
import app.usenekko.shared.contacts.ImportedContact
import app.usenekko.shared.paywall.PaywallGateManager
import app.usenekko.shared.paywall.PaywallTrigger
import app.usenekko.shared.subscription.GateResult
import app.usenekko.shared.subscription.SubscriptionGates
import app.usenekko.shared.subscription.SubscriptionRepository
import app.usenekko.shared.notifications.ReminderScheduler
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddContactViewModel(
    private val contactDataSource: ContactDataSource,
    private val reminderScheduler: ReminderScheduler,
    private val subscriptionRepository: SubscriptionRepository,
    private val homeRepository: HomeRepository? = null,
    private val paywallGateManager: PaywallGateManager? = null,
    editingContact: Contact? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState(editingContact))
    val state: StateFlow<AddContactState> = _state.asStateFlow()

    private val _events = Channel<AddContactEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (homeRepository != null) {
            _state.update { it.copy(groupsLoading = true) }
            observeHomeGroups()
        }
        loadGroups()
    }

    private fun observeHomeGroups() {
        viewModelScope.launch {
            homeRepository?.groupPickerState?.collectLatest { pickerState ->
                applyHomeGroups(pickerState)
            }
        }
    }

    private fun applyHomeGroups(pickerState: HomeGroupPickerState) {
        _state.update { state ->
            state.copy(
                groups = pickerState.groups,
                contacts = pickerState.contacts,
                memberships = pickerState.memberships,
                selectedGroupId = state.selectedGroupId ?: state.editingContactId?.let { contactId ->
                    pickerState.memberships.firstOrNull { it.contactId == contactId }?.groupId
                },
                initialGroupId = if (state.initialGroupResolved) {
                    state.initialGroupId
                } else {
                    pickerState.memberships.firstOrNull { it.contactId == state.editingContactId }?.groupId
                },
                initialGroupResolved = state.initialGroupResolved || !pickerState.isLoading,
                groupsLoading = pickerState.isLoading,
                error = pickerState.error?.toString(),
            )
        }
    }

    fun onNameChanged(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun onAvatarSelected(index: Int) {
        _state.update { it.copy(selectedAvatarIndex = index, importedPhoto = null, error = null) }
    }

    fun onContactImported(contact: ImportedContact) {
        _state.update { it.withImportedContact(contact) }
    }

    fun onFrequencySelected(frequency: String) {
        _state.update { it.copy(selectedFrequency = frequency, error = null) }
    }

    fun onTimeSelected(hour: Int, minute: Int, isAm: Boolean) {
        _state.update { it.copy(selectedHour = hour, selectedMinute = minute, isAm = isAm, error = null) }
    }

    fun onTimeDialChanged(totalMinutes: Int) {
        _state.update { it.withTimeDialValue(totalMinutes).copy(error = null) }
    }

    fun onGroupSelected(groupId: String) {
        _state.update { it.copy(selectedGroupId = groupId, error = null) }
    }

    fun onNextStep() {
        _state.update { state ->
            if (state.canAdvanceFromStep && !state.isSubmitting) {
                state.copy(currentStep = state.currentStep + 1, error = null)
            } else {
                state
            }
        }
    }

    fun onBackStep() {
        _state.update { state ->
            state.copy(currentStep = (state.currentStep - 1).coerceAtLeast(0), error = null)
        }
    }

    fun resetDraft() {
        _state.update { current ->
            // Keep the group catalog so step 1 stays populated on the next run;
            // applyHomeGroups keeps it in sync with future repository emissions.
            AddContactState().copy(
                groups = current.groups,
                contacts = current.contacts,
                memberships = current.memberships,
                initialGroupResolved = current.initialGroupResolved,
            )
        }
    }

    fun onCreateGroupClicked() {
        _state.update { it.copy(showCreateGroupSheet = true) }
    }

    fun onDismissCreateGroupSheet() {
        _state.update { it.copy(showCreateGroupSheet = false) }
    }

    fun onSaveGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || _state.value.isCreatingGroup) return

        viewModelScope.launch {
            _state.update { it.copy(isCreatingGroup = true, error = null) }

            when (val result = contactDataSource.createGroup(name = trimmed, color = null)) {
                is Result.Success -> {
                    _state.update { state ->
                        state.copy(
                            groups = state.groups + result.data,
                            selectedGroupId = result.data.id,
                            showCreateGroupSheet = false,
                            isCreatingGroup = false,
                        )
                    }
                    homeRepository?.invalidate()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(isCreatingGroup = false, error = result.error.toString())
                    }
                }
            }
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            if (homeRepository != null) {
                homeRepository.load()
                return@launch
            }

            _state.update { it.copy(groupsLoading = true) }

            val groupsResult = contactDataSource.getGroups()
            val contactsResult = contactDataSource.getContacts()
            val membershipsResult = contactDataSource.getGroupMemberships()
            _state.update { state ->
                state.copy(
                    groups = (groupsResult as? Result.Success)?.data ?: state.groups,
                    contacts = (contactsResult as? Result.Success)?.data ?: state.contacts,
                    memberships = (membershipsResult as? Result.Success)?.data ?: state.memberships,
                    selectedGroupId = state.selectedGroupId ?: state.editingContactId?.let { contactId ->
                        (membershipsResult as? Result.Success)?.data
                            ?.firstOrNull { it.contactId == contactId }
                            ?.groupId
                    },
                    initialGroupId = if (state.initialGroupResolved) {
                        state.initialGroupId
                    } else {
                        (membershipsResult as? Result.Success)?.data
                            ?.firstOrNull { it.contactId == state.editingContactId }
                            ?.groupId
                    },
                    initialGroupResolved = state.initialGroupResolved || membershipsResult is Result.Success,
                    groupsLoading = false,
                )
            }
        }
    }

    fun submit() {
        val state = _state.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }

            // Gate 1 — Unlimited Contacts: free users capped at 10.
            // Block + show paywall (never silently fail) when the limit is hit.
            val isSubscribed = subscriptionRepository.isSubscribed.value
            if (state.editingContactId == null && !isSubscribed) {
                when (val contactsResult = contactDataSource.getContacts()) {
                    is Result.Success -> {
                        val gate = SubscriptionGates.contactGate(
                            isSubscribed = isSubscribed,
                            currentContactCount = contactsResult.data.size,
                        )
                        if (gate is GateResult.Blocked) {
                            _state.update { it.copy(isSubmitting = false) }

                            // LIMIT_HIT trigger: let the discount-gate engine decide
                            // whether this moment earns the 60%-off impression.
                            val showDiscount = paywallGateManager?.reportTrigger(PaywallTrigger.LIMIT_HIT) == true
                            _events.send(AddContactEvent.ShowPaywall(gate.reason, showDiscount))
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        // Can't count contacts — proceed cautiously (server insert still runs).
                    }
                }
            }

            val colorHex = colorHexes[state.selectedAvatarIndex ?: 0]
            val reminderTime = formatTime(state.selectedHour, state.selectedMinute, state.isAm)

            val result = if (state.editingContactId == null) {
                contactDataSource.createContact(
                    name = state.name.trim(),
                    avatarColor = colorHex,
                    checkInFrequency = state.selectedFrequency,
                    reminderTime = reminderTime,
                )
            } else {
                contactDataSource.updateContact(
                    contactId = state.editingContactId,
                    name = state.name.trim(),
                    avatarColor = colorHex,
                    checkInFrequency = state.selectedFrequency,
                    reminderTime = reminderTime,
                )
            }

            when (result) {
                is Result.Success -> {
                    val saved = result.data
                    val groupResult = if (state.editingContactId == null) {
                        state.selectedGroupId?.let { groupId ->
                            contactDataSource.assignContactToGroup(saved.id, groupId)
                        }
                    } else {
                        syncGroupMembership(saved.id, state.selectedGroupId, state.memberships)
                    }

                    if (groupResult is Result.Error) {
                        _state.update { it.copy(isSubmitting = false, error = groupResult.error.toString()) }
                        return@launch
                    }

                    homeRepository?.invalidate()
                    if (state.editingContactId == null) {
                        // Schedule the first reminder locally — never server-sent.
                        scheduleFirstReminder(saved)
                    } else {
                        rescheduleReminder(saved)
                    }
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(AddContactEvent.Saved)
                }
                is Result.Error -> {
                    _state.update { it.copy(isSubmitting = false, error = result.error.toString()) }
                }
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int, isAm: Boolean): String {
        val hour24 = when {
            isAm && hour == 12 -> 0
            isAm -> hour
            hour == 12 -> 12
            else -> hour + 12
        }
        val hh = hour24.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')
        return "$hh:$mm:00"
    }

    private fun scheduleFirstReminder(contact: Contact) {
        // Brand-new contact: the first reminder fires at the next occurrence of
        // the picked reminder time (today if not yet passed, else tomorrow), so
        // a "few minutes from now" test fires the same day. Follow-up reminders
        // drive off next_check_in_date after the first real check-in.
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            if (!reminderScheduler.isEnabled()) return@launch
            reminderScheduler.cancel(contact.id)
            contact.initialReminder(now)?.let { reminder ->
                reminderScheduler.schedule(
                    reminder.contactId,
                    reminder.contactName,
                    reminder.fireAtEpochMillis,
                )
            }
        }
    }

    private suspend fun syncGroupMembership(
        contactId: String,
        selectedGroupId: String?,
        memberships: List<GroupMembership>,
    ): Result<Unit, ContactError>? {
        val previousGroupId = memberships.firstOrNull { it.contactId == contactId }?.groupId
        return when {
            previousGroupId == selectedGroupId -> null
            previousGroupId == null && selectedGroupId != null ->
                contactDataSource.assignContactToGroup(contactId, selectedGroupId)
            previousGroupId != null && selectedGroupId == null ->
                contactDataSource.removeContactFromGroup(contactId, previousGroupId)
            previousGroupId != null && selectedGroupId != null ->
                contactDataSource.moveContactToGroup(contactId, previousGroupId, selectedGroupId)
            else -> null
        }
    }

    private fun rescheduleReminder(contact: Contact) {
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            if (!reminderScheduler.isEnabled()) return@launch
            reminderScheduler.cancel(contact.id)
            (contact.nextReminder(now) ?: contact.initialReminder(now))?.let { reminder ->
                reminderScheduler.schedule(
                    reminder.contactId,
                    reminder.contactName,
                    reminder.fireAtEpochMillis,
                )
            }
        }
    }

    companion object {
        val colorHexes = listOf(
            "#FFCC33",
            "#34C759",
            "#FF9500",
            "#FF3B30",
            "#AF52DE",
            "#007AFF",
        )

        private fun initialState(contact: Contact?): AddContactState {
            if (contact == null) return AddContactState()
            val time = parseTime(contact.reminderTime)
            return AddContactState(
                editingContactId = contact.id,
                name = contact.name,
                initialName = contact.name,
                selectedAvatarIndex = colorHexes.indexOf(contact.avatarColor).takeIf { it >= 0 } ?: 0,
                initialAvatarIndex = colorHexes.indexOf(contact.avatarColor).takeIf { it >= 0 } ?: 0,
                selectedFrequency = contact.checkInFrequency,
                initialFrequency = contact.checkInFrequency,
                selectedHour = time.first,
                selectedMinute = time.second,
                isAm = time.third,
                initialHour = time.first,
                initialMinute = time.second,
                initialIsAm = time.third,
                initialGroupResolved = false,
            )
        }

        private fun parseTime(value: String?): Triple<Int, Int, Boolean> {
            val hour24 = value?.substringBefore(":")?.toIntOrNull()?.coerceIn(0, 23) ?: 22
            val minute = value?.substringAfter(":")?.substringBefore(":")?.toIntOrNull()
                ?.coerceIn(0, 59) ?: 30
            return Triple(
                when (hour24 % 12) {
                    0 -> 12
                    else -> hour24 % 12
                },
                minute,
                hour24 < 12,
            )
        }
    }
}
