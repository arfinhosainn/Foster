package app.usefoster.home.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usefoster.home.data.HomeRepository
import app.usefoster.home.data.HomeSnapshot
import app.usefoster.home.presentation.components.TimelineSlot
import app.usefoster.home.presentation.components.resolveInitialCountdownStartDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the check-in history screen. Observes the shared [HomeRepository] Home
 * already loads — no additional network calls. Raw check-ins are reduced to
 * LocalDate lookup maps once per snapshot ([HistoryLookupMaps]); board models
 * are pure map lookups afterwards.
 */
class CheckInHistoryViewModel(
    private val homeRepository: HomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckInHistoryState())
    val state: StateFlow<CheckInHistoryState> = _state.asStateFlow()

    private val _selectedDot = MutableStateFlow<DotDetails?>(null)
    val selectedDot: StateFlow<DotDetails?> = _selectedDot.asStateFlow()

    /** Lookup maps + snapshot cached so dot taps don't re-derive anything heavy. */
    private var lookupMaps: HistoryLookupMaps? = null
    private var lastSnapshot: HomeSnapshot? = null

    init {
        observeRepository()
        // Repository-level dedupe (in-flight deferred + mutex) makes this safe
        // to call unconditionally, even when Home is loading concurrently.
        load()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            homeRepository.state.collect { repositoryState ->
                applySnapshot(repositoryState.snapshot)
            }
        }
    }

    /**
     * Requests a load. Idempotent: the repository returns the cached snapshot
     * when fresh and joins the in-flight request when one is running.
     */
    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            homeRepository.load(forceRefresh)
        }
    }

    fun selectDot(board: HistoryBoardUiModel, slot: TimelineSlot) {
        val snapshot = lastSnapshot ?: return
        val maps = lookupMaps ?: return
        _selectedDot.value = buildDotDetails(
            checkIns = snapshot.checkInHistory,
            maps = maps,
            contacts = snapshot.contacts,
            board = board,
            slot = slot,
        )
    }

    fun dismissDot() {
        _selectedDot.value = null
    }

    private fun applySnapshot(snapshot: HomeSnapshot?) {
        if (snapshot == null) {
            _state.value = CheckInHistoryState(isLoading = true)
            return
        }

        if (snapshot !== lastSnapshot) {
            lookupMaps = buildHistoryLookupMaps(
                checkIns = snapshot.checkInHistory,
                missedCheckIns = snapshot.missedCheckIns,
            )
            lastSnapshot = snapshot
        }
        val maps = lookupMaps ?: return

        val anchor = resolveInitialCountdownStartDate(
            existingStartDate = null,
            checkIns = snapshot.checkInHistory,
            contacts = snapshot.contacts,
            today = snapshot.localDate,
            missedCheckIns = snapshot.missedCheckIns,
        )
        val boards = if (anchor == null) {
            emptyList()
        } else {
            buildBoardUiModels(
                maps = maps,
                contacts = snapshot.contacts,
                anchor = anchor,
                today = snapshot.localDate,
            )
        }
        val progress = anchor?.let {
            currentBoardProgress(
                maps = maps,
                contactIds = snapshot.contacts.mapTo(mutableSetOf()) { it.id },
                anchor = it,
                today = snapshot.localDate,
            )
        }

        _state.value = CheckInHistoryState(
            isLoading = false,
            boards = boards,
            currentBoardProgress = progress,
        )
    }
}
