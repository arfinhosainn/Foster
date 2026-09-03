package app.usefoster.home

import app.usefoster.home.data.HomeGroupPickerState
import app.usefoster.home.data.HomeRepository
import app.usefoster.home.data.HomeRepositoryState
import app.usefoster.home.data.HomeSnapshot
import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactError
import app.usefoster.home.presentation.components.TIMELINE_SLOT_COUNT
import app.usefoster.home.presentation.components.buildTimelineSlots
import app.usefoster.home.presentation.history.CheckInHistoryViewModel
import app.usefoster.shared.domain.Result
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInHistoryViewModelTest {

    private val today = LocalDate(2026, 9, 15) // 36 days after anchor → board 2 is current

    private fun contact(id: String = "c1") = Contact(
        id = id,
        name = "C$id",
        avatarColor = "#007AFF",
        checkInFrequency = "daily",
        reminderTime = "12:00:00",
        nextCheckInDate = null,
        lastCheckInDate = null,
        streakCount = 0,
    )

    private fun checkIn(contactId: String, date: LocalDate) = CheckIn(
        id = "$contactId-$date",
        contactId = contactId,
        checkedInAt = "${date}T12:00:00Z",
    )

    /** Minimal repository fake: pushes a snapshot into state on load(). */
    private class FakeHomeRepository : HomeRepository {
        private val _state = MutableStateFlow(HomeRepositoryState())
        override val state: StateFlow<HomeRepositoryState> = _state.asStateFlow()
        override val groupPickerState: Flow<HomeGroupPickerState> = flowOf(HomeGroupPickerState())

        var snapshot: HomeSnapshot? = null

        override suspend fun load(forceRefresh: Boolean): Result<HomeSnapshot, ContactError> {
            val snap = snapshot ?: return Result.Error(ContactError.Network)
            _state.value = HomeRepositoryState(snapshot = snap)
            return Result.Success(snap)
        }

        override fun invalidate() {}
    }

    private fun snapshot(checkIns: List<CheckIn>): HomeSnapshot = HomeSnapshot(
        contacts = checkIns.map { it.contactId }.distinct().map { contact(it) },
        groups = emptyList(),
        memberships = emptyList(),
        recentCheckIns = emptyList(),
        checkInHistory = checkIns,
        missedCheckIns = emptyList(),
        fetchedAt = Clock.System.now(),
        accountKey = "test",
        localDate = today,
    )

    @Test
    fun isLoadingUntilSnapshotArrives() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeHomeRepository()
            val viewModel = CheckInHistoryViewModel(repository)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun derivesFinishedBoardsFromRepositorySnapshot() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeHomeRepository()
            // Anchor = Aug 10 (earliest check-in). Board 1: Aug 10 – Sep 4.
            // Sep 10 falls into board 2, which contains today (Sep 15) → excluded.
            repository.snapshot = snapshot(
                listOf(
                    checkIn("c1", LocalDate(2026, 8, 10)),
                    checkIn("c1", LocalDate(2026, 9, 10)),
                ),
            )
            val viewModel = CheckInHistoryViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(!state.isLoading)
            assertEquals(1, state.boards.size)
            assertEquals(1, state.boards.single().boardIndex)
            assertEquals(LocalDate(2026, 8, 10), state.boards.single().startDate)
            assertEquals(1, state.boardsFilled)
            // One check-in, zero missed occurrences → counts as perfect.
            assertEquals(1, state.perfectCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun perfectBoardCountedInStats() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeHomeRepository()
            // 26 consecutive check-ins from Aug 10 → board 1 is perfect.
            val checkIns = (0 until TIMELINE_SLOT_COUNT).map { offset ->
                checkIn("c1", LocalDate(2026, 8, 10).plus(DatePeriod(days = offset)))
            }
            repository.snapshot = snapshot(checkIns)
            val viewModel = CheckInHistoryViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(1, state.boardsFilled)
            assertEquals(1, state.perfectCount)
            assertTrue(state.boards.single().isPerfect)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun selectDotExposesDotDetailsAndDismissClears() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeHomeRepository()
            repository.snapshot = snapshot(
                listOf(checkIn("c1", LocalDate(2026, 8, 10))),
            )
            val viewModel = CheckInHistoryViewModel(repository)
            advanceUntilIdle()

            val board = viewModel.state.value.boards.single()
            val slots = buildTimelineSlots(
                startDate = board.startDate,
                today = today,
                events = board.events,
            )
            viewModel.selectDot(board, slots.first())
            advanceUntilIdle()

            val dot = viewModel.selectedDot.value
            assertNotNull(dot)
            assertEquals(1, dot!!.boardIndex)
            assertEquals(1, dot.dotNumber)
            assertEquals(LocalDate(2026, 8, 10), dot.date)
            assertEquals("Cc1", dot.checkIns.single().contactName)
            assertNotNull(dot.checkIns.single().time)

            viewModel.dismissDot()
            assertNull(viewModel.selectedDot.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptyHistoryYieldsNoBoards() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeHomeRepository()
            repository.snapshot = snapshot(emptyList())
            val viewModel = CheckInHistoryViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(!state.isLoading)
            assertEquals(0, state.boards.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun firstBoardInProgressShowsProgressInsteadOfBoards() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeHomeRepository()
            // Anchor = Aug 10 (earliest check-in); today = Aug 20 → only 10
            // days elapsed, so board 1 is still current and nothing is archived.
            repository.snapshot = snapshot(
                listOf(
                    checkIn("c1", LocalDate(2026, 8, 10)),
                    checkIn("c1", LocalDate(2026, 8, 12)),
                ),
            ).copy(localDate = LocalDate(2026, 8, 20))
            val viewModel = CheckInHistoryViewModel(repository)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(!state.isLoading)
            assertEquals(0, state.boards.size) // nothing archived yet
            assertEquals(2, state.currentBoardProgress) // 2 dots filled live
        } finally {
            Dispatchers.resetMain()
        }
    }
}
