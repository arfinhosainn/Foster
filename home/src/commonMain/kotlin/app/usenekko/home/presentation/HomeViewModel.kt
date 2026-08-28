package app.usenekko.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.data.HomeSnapshot
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.domain.contactIdsCheckedInOn
import app.usenekko.home.domain.computeCheckInUpdate
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.data.AccountRepository
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.domain.NotificationReconciler
import app.usenekko.home.domain.Reminder
import app.usenekko.home.domain.buildDuePlan
import app.usenekko.home.domain.isOutstanding
import app.usenekko.home.domain.isCheckedInToday
import app.usenekko.home.presentation.badges.detectAndTriggerBadgeReveal
import app.usenekko.home.presentation.badges.unlockedBadgeIdsOrNull
import app.usenekko.home.presentation.components.resolveInitialCountdownStartDate
import app.usenekko.shared.domain.Result
import app.usenekko.shared.notifications.ReconcileSource
import app.usenekko.shared.notifications.ReminderScheduler
import app.usenekko.shared.paywall.PaywallGateManager
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
    private val accountRepository: AccountRepository? = null,
    private val paywallGateManager: PaywallGateManager? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var allContacts: List<Contact> = emptyList()
    private var memberships: List<GroupMembership> = emptyList()
    private var customReminders: List<Reminder> = emptyList()
    private val notificationReconciler = NotificationReconciler(reminderScheduler)
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
        val previousContactsById = allContacts.associateBy(Contact::id)
        allContacts = snapshot.contacts.map { incoming ->
            incoming.copy(
                avatarColor = incoming.avatarColor
                    ?: previousContactsById[incoming.id]?.avatarColor,
            )
        }
        memberships = snapshot.memberships
        customReminders = snapshot.customReminders

        val localToday = today()
        val checkedInTodayContactIds = snapshot.recentCheckIns.contactIdsCheckedInOn(localToday) +
            allContacts.filter { it.isCheckedInToday(localToday) }.map { it.id }
        val checkInCounts = snapshot.checkInHistory
            .groupingBy { it.contactId }
            .eachCount()
        val initialCountdownStartDate = resolveInitialCountdownStartDate(
            existingStartDate = _state.value.initialCountdownStartDate,
            checkIns = snapshot.checkInHistory,
            contacts = snapshot.contacts,
            today = localToday,
            missedCheckIns = snapshot.missedCheckIns,
        )
        val effectiveGroupId = _state.value.selectedGroupId
            ?.takeIf { selected -> snapshot.groups.any { it.id == selected } }

        _state.value = _state.value.copy(
            isLoading = false,
            groups = snapshot.groups,
            selectedGroupId = effectiveGroupId,
            checkIns = snapshot.checkInHistory,
            missedCheckIns = snapshot.missedCheckIns,
            checkInCounts = checkInCounts,
            initialCountdownStartDate = initialCountdownStartDate,
            error = null,
        )
        recomputeCounts(allContacts, effectiveGroupId, memberships, checkedInTodayContactIds)
        reconcileReminders()
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
        if (_state.value.checkingInContactIds.contains(contactId)) return
        val today = today()
        val contact = allContacts.firstOrNull { it.id == contactId }
        if (contact == null || !contact.isOutstanding(today) || contact.isCheckedInToday(today)) return

        viewModelScope.launch {
            // Re-check under the lock once the coroutine starts: another tap of the
            // same contact may have already begun since the guard above ran.
            if (_state.value.checkingInContactIds.contains(contactId)) return@launch

            val update = computeCheckInUpdate(contact, today)
            val now = Clock.System.now()
            val nowIso = now.toString()
            // logCheckIn returns only the updated contact, not the created check-in
            // row, so append a temporary local entry after the request succeeds.
            // Reconciliation later swaps its id for the server row without duplicating.
            val tempCheckIn = CheckIn(
                id = "temp-$contactId-${now.toEpochMilliseconds()}",
                contactId = contactId,
                checkedInAt = nowIso,
            )

            // Capture the exact pre-tap snapshot so failure can restore the loading
            // state cleanly instead of recomputing (which could drift from the real
            // pre-state).
            val preState = _state.value
            val preAllContacts = allContacts

            // Mark the request in flight without changing the contact or history yet.
            _state.value = preState.copy(
                checkingInContactIds = preState.checkingInContactIds + contactId,
                checkInError = null,
            )

            val previousBadges = contactDataSource.unlockedBadgeIdsOrNull()

            val result = contactDataSource.logCheckIn(
                contactId = contact.id,
                lastCheckInDate = update.lastCheckInDate,
                nextCheckInDate = update.nextCheckInDate,
                streakCount = update.streakCount,
                checkedInAt = nowIso,
            )

            when (result) {
                is Result.Success -> {
                    allContacts = allContacts.map { existing ->
                        if (existing.id == result.data.id) {
                            result.data.copy(avatarColor = result.data.avatarColor ?: existing.avatarColor)
                        } else {
                            existing
                        }
                    }
                    val completedState = _state.value.copy(
                        allContacts = allContacts,
                        contacts = _state.value.contacts.map { existing ->
                            allContacts.firstOrNull { it.id == existing.id } ?: existing
                        },
                        checkIns = _state.value.checkIns + tempCheckIn,
                        checkingInContactIds = _state.value.checkingInContactIds - contactId,
                    )
                    _state.value = completedState
                    recomputeCounts(
                        contacts = allContacts,
                        groupId = _state.value.selectedGroupId,
                        memberships = memberships,
                        checkedInTodayContactIds = completedState.checkIns.contactIdsCheckedInOn(today) +
                            allContacts.filter { it.isCheckedInToday(today) }.map { it.id },
                    )
                    if (previousBadges != null) {
                        contactDataSource.detectAndTriggerBadgeReveal(previousBadges)
                    }
                    // Do NOT force a full home reload here: we already have the
                    // authoritative contact from logCheckIn. Invalidate instead so the
                    // next staleness-triggered load reconciles silently in the
                    // background, without the "whole screen refresh" flash.
                    homeRepository.invalidate()
                    accountRepository?.invalidate()
                    // AHA_MOMENT: a completed check-in is core value — let the
                    // gate engine decide whether this moment earns the 60%-off
                    // impression (silently ignored when gates fail).
                    paywallGateManager?.let { manager ->
                        launch { manager.onCoreValueActionCompleted() }
                    }
                }
                is Result.Error -> {
                    // Exact rollback from the captured pre-tap snapshot.
                    allContacts = preAllContacts
                    _state.value = preState.copy(
                        checkingInContactIds = preState.checkingInContactIds - contactId,
                        checkInError = result.error.toString(),
                    )
                    recomputeCounts(
                        contacts = preAllContacts,
                        groupId = preState.selectedGroupId,
                        memberships = memberships,
                        checkedInTodayContactIds = preState.checkIns.contactIdsCheckedInOn(today) +
                            preAllContacts.filter { it.isCheckedInToday(today) }.map { it.id },
                    )
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
            allContacts = contacts,
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
     * App-launch / data-change reconciliation: rebuild the full due plan from
     * current data and apply it through [NotificationReconciler], which owns
     * the day-digest grouping, delivered-day idempotency, and cancel/schedule
     * operations. Calling it repeatedly never duplicates notifications.
     */
    private fun reconcileReminders() {
        viewModelScope.launch {
            if (!reminderScheduler.isEnabled()) return@launch
            val now = Clock.System.now().toEpochMilliseconds()
            val localToday = today()
            val checkedInToday = _state.value.checkIns.contactIdsCheckedInOn(localToday) +
                allContacts.filter { it.isCheckedInToday(localToday) }.map { it.id }
            val duePlan = buildDuePlan(
                contacts = allContacts,
                customReminders = customReminders,
                today = localToday,
                nowEpochMillis = now,
                checkedInTodayContactIds = checkedInToday,
            )
            notificationReconciler.reconcile(duePlan, now, ReconcileSource.FOREGROUND)
        }
    }
}
