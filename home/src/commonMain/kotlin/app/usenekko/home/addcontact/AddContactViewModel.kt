package app.usenekko.home.addcontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.initialReminder
import app.usenekko.shared.domain.Result
import app.usenekko.shared.subscription.GateResult
import app.usenekko.shared.subscription.SubscriptionGates
import app.usenekko.shared.subscription.SubscriptionRepository
import app.usenekko.shared.notifications.ReminderScheduler
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddContactViewModel(
    private val contactDataSource: ContactDataSource,
    private val reminderScheduler: ReminderScheduler,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddContactState())
    val state: StateFlow<AddContactState> = _state.asStateFlow()

    private val _events = Channel<AddContactEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadGroups()
    }

    fun onNameChanged(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun onColorSelected(index: Int) {
        _state.update { it.copy(selectedColorIndex = index, error = null) }
    }

    fun onFrequencySelected(frequency: String) {
        _state.update { it.copy(selectedFrequency = frequency, error = null) }
    }

    fun onTimeSelected(hour: Int, minute: Int, isAm: Boolean) {
        _state.update { it.copy(selectedHour = hour, selectedMinute = minute, isAm = isAm, error = null) }
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
            _state.update { it.copy(groupsLoading = true) }

            when (val result = contactDataSource.getGroups()) {
                is Result.Success -> {
                    _state.update { it.copy(groups = result.data, groupsLoading = false) }
                }
                is Result.Error -> {
                    _state.update { it.copy(groupsLoading = false) }
                }
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
            if (!isSubscribed) {
                when (val contactsResult = contactDataSource.getContacts()) {
                    is Result.Success -> {
                        val gate = SubscriptionGates.contactGate(
                            isSubscribed = isSubscribed,
                            currentContactCount = contactsResult.data.size,
                        )
                        if (gate is GateResult.Blocked) {
                            _state.update { it.copy(isSubmitting = false) }
                            _events.send(AddContactEvent.ShowPaywall(gate.reason))
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        // Can't count contacts — proceed cautiously (server insert still runs).
                    }
                }
            }

            val colorHex = colorHexes[state.selectedColorIndex ?: 0]
            val reminderTime = formatTime(state.selectedHour, state.selectedMinute, state.isAm)

            when (
                val result = contactDataSource.createContact(
                    name = state.name.trim(),
                    avatarColor = colorHex,
                    checkInFrequency = state.selectedFrequency,
                    reminderTime = reminderTime,
                )
            ) {
                is Result.Success -> {
                    val created = result.data
                    val groupId = _state.value.selectedGroupId
                    if (groupId != null) {
                        contactDataSource.assignContactToGroup(
                            contactId = created.id,
                            groupId = groupId,
                        )
                    }
                    // Schedule the first reminder locally — never server-sent.
                    scheduleFirstReminder(created)
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

    companion object {
        val colorHexes = listOf(
            "#FFCC33",
            "#34C759",
            "#FF9500",
            "#FF3B30",
            "#AF52DE",
            "#007AFF",
        )
    }
}
