package app.usenekko.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.data.HomeSnapshot
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.domain.contactIdsCheckedInOn
import app.usenekko.home.domain.computeCheckInUpdate
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.domain.computeReminderPlans
import app.usenekko.home.domain.isOutstanding
import app.usenekko.home.domain.isCheckedInToday
import app.usenekko.home.presentation.badges.detectAndTriggerBadgeReveal
import app.usenekko.home.presentation.badges.unlockedBadgeIdsOrNull
import app.usenekko.shared.domain.Result
import app.usenekko.shared.notifications.ReminderScheduler
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class HomeViewModel(
    private val contactDataSource: ContactDataSource,
    private val reminderScheduler: ReminderScheduler,
    homeRepository: HomeRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var allContacts: List<Contact> = emptyList()
    private var memberships: List<GroupMembership> = emptyList()
    private val homeRepository: HomeRepository
    private var hasLoadedRepository = false

    init {
        this.homeRepository = homeRepository ?: InMemoryHomeRepository(
            contactDataSource = contactDataSource,
            accountKeyProvider = { "default" },
            scope = viewModelScope,
        )
        observeRepository()
        loadContacts()
    }

    fun loadContacts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (homeRepository.state.value.snapshot == null) {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }

            when (val result = homeRepository.load(forceRefresh)) {
                is Result.Success -> applySnapshot(result.data)
                is Result.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = result.error.toString(),
                )
            }
            hasLoadedRepository = true
        }
    }

    fun refreshIfStale() {
        loadContacts()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            homeRepository.state.drop(1).collect { repositoryState ->
                if (!hasLoadedRepository) return@collect
                repositoryState.snapshot?.let(::applySnapshot)
                _state.value = _state.value.copy(
                    isRefreshing = repositoryState.isRefreshing,
                    error = repositoryState.error?.toString(),
                )
            }
        }
    }

    private fun applySnapshot(snapshot: HomeSnapshot) {
        allContacts = snapshot.contacts
        memberships = snapshot.memberships

        val checkedInTodayContactIds = snapshot.recentCheckIns.contactIdsCheckedInOn(today()) +
            allContacts.filter { it.isCheckedInToday(today()) }.map { it.id }
        val checkInCounts = snapshot.checkInHistory
            .groupingBy { it.contactId }
            .eachCount()
        val effectiveGroupId = _state.value.selectedGroupId
            ?.takeIf { selected -> snapshot.groups.any { it.id == selected } }

        _state.value = _state.value.copy(
            isLoading = false,
            groups = snapshot.groups,
            selectedGroupId = effectiveGroupId,
            checkIns = snapshot.recentCheckIns,
            checkInCounts = checkInCounts,
            error = null,
        )
        recomputeCounts(allContacts, effectiveGroupId, memberships, checkedInTodayContactIds)
        reconcileReminders(allContacts)
    }

    fun onGroupSelected(groupId: String?) {
        if (_state.value.selectedGroupId == groupId) return
        _state.value = _state.value.copy(selectedGroupId = groupId)
        recomputeCounts(
            contacts = allContacts,
            groupId = groupId,
            memberships = memberships,
            checkedInTodayContactIds = _state.value.checkIns.contactIdsCheckedInOn(today()),
        )
    }

    fun checkIn(contactId: String) {
        if (_state.value.checkingInContactId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(checkingInContactId = contactId, checkInError = null)

            val contact = allContacts.firstOrNull { it.id == contactId }
            if (contact == null) {
                _state.value = _state.value.copy(checkingInContactId = null)
                return@launch
            }

            val previousBadges = contactDataSource.unlockedBadgeIdsOrNull()

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val update = computeCheckInUpdate(contact, today)
            val result = contactDataSource.logCheckIn(
                contactId = contact.id,
                lastCheckInDate = update.lastCheckInDate,
                nextCheckInDate = update.nextCheckInDate,
                streakCount = update.streakCount,
            )

            _state.value = _state.value.copy(checkingInContactId = null)
            when (result) {
                is Result.Success -> {
                    if (previousBadges != null) {
                        contactDataSource.detectAndTriggerBadgeReveal(previousBadges)
                    }
                    homeRepository.invalidate()
                    loadContacts(forceRefresh = true)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(checkInError = result.error.toString())
                }
            }
        }
    }

    private fun recomputeCounts(
        contacts: List<Contact>,
        groupId: String?,
        memberships: List<GroupMembership>,
        checkedInTodayContactIds: Set<String>,
    ) {
        val today = today()
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
            outstandingCount = filtered.count {
                it.isOutstanding(today) && it.id !in checkedInTodayContactIds
            },
            upToDateCount = filtered.count {
                !it.isOutstanding(today) || it.id in checkedInTodayContactIds
            },
            contacts = filtered,
        )
    }

    private fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    /**
     * App-launch / data-change reconciliation: cancel every contact's alarm, then
     * re-schedule from the current DB state. This keeps things simple and idempotent —
     * calling it again never duplicates notifications because we first cancel all.
     *
     * A brand-new contact has `next_check_in_date` still null (Home's "null =
     * outstanding" split depends on that), so it falls back to its initial reminder
     * (created + one cadence). Without this, reconciliation would cancel the
     * creation-time alarm and never re-schedule it — a fresh contact's first
     * reminder would never fire.
     *
     * iOS only allows 64 pending notifications; [takeSoonest] keeps the soonest
     * [app.usenekko.shared.notifications.MaxPendingReminders] by fire time.
     */
    private fun reconcileReminders(contacts: List<Contact>) {
        viewModelScope.launch {
            if (!reminderScheduler.isEnabled()) return@launch
            val now = Clock.System.now().toEpochMilliseconds()
            val plans = contacts.computeReminderPlans(now)
            // Cancel all first (a contact leaving the planned set must drop its alarm).
            contacts.forEach { reminderScheduler.cancel(it.id) }
            plans.forEach { reminderScheduler.schedule(it.contactId, it.contactName, it.fireAtEpochMillis) }
        }
    }
}
