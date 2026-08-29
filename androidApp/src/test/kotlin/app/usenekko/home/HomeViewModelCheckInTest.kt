package app.usenekko.home

import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.ContactError
import app.usenekko.home.domain.localDate
import app.usenekko.home.data.InMemoryAccountRepository
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.domain.forTodayCheckInList
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.home.presentation.components.buildCheckInTimelineEvents
import app.usenekko.home.presentation.components.buildTimelineSlots
import app.usenekko.home.presentation.components.timelineRenderedAvatarCount
import app.usenekko.home.presentation.components.timelineStartForToday
import app.usenekko.shared.notifications.ReminderScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCheckInTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun contact(
        id: String,
        frequency: String = "daily",
        next: String = today.minus(DatePeriod(days = 1)).toString(),
    ) = Contact(
        id = id,
        name = "C$id",
        avatarColor = "#007AFF",
        checkInFrequency = frequency,
        reminderTime = "12:00:00",
        nextCheckInDate = next,
        lastCheckInDate = null,
        streakCount = 0,
    )

    @Test
    fun checkInFlipsOutstandingToUpToDate() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1")))
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(0, viewModel.state.value.upToDateCount)

            viewModel.checkIn("c1")
            advanceUntilIdle()

            val call = dataSource.logCheckInCalls.single()
            assertEquals("c1", call.contactId)
            assertEquals(today.toString(), call.lastCheckInDate)
            assertEquals(today.plus(DatePeriod(days = 1)).toString(), call.nextCheckInDate)
            assertEquals(1, call.streakCount)

            assertEquals(0, viewModel.state.value.outstandingCount)
            assertEquals(1, viewModel.state.value.upToDateCount)
            assertEquals(1, viewModel.state.value.checkIns.size)
            assertEquals("c1", viewModel.state.value.checkIns.first().contactId)
            assertTrue(viewModel.state.value.checkingInContactIds.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun checkInKeepsContactInOriginalPositionUntilRequestCompletes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val checkInGate = CompletableDeferred<Unit>()
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("first"), contact("second")),
            ).apply {
                this.checkInGate = checkInGate
            }
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("first")
            runCurrent()

            assertEquals(setOf("first"), viewModel.state.value.checkingInContactIds)
            assertEquals(
                listOf("first", "second"),
                viewModel.state.value.contacts.forTodayCheckInList(today).map { it.id },
            )
            assertEquals(null, viewModel.state.value.contacts.first { it.id == "first" }.lastCheckInDate)
            assertTrue(viewModel.state.value.checkIns.isEmpty())

            checkInGate.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.checkingInContactIds.isEmpty())
            assertEquals(
                listOf("second", "first"),
                viewModel.state.value.contacts.forTodayCheckInList(today).map { it.id },
            )
            assertEquals(1, viewModel.state.value.checkIns.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun successfulCheckInPassesDeviceTimestampToHistoryWriter() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1")))
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()

            val checkedInAt = dataSource.logCheckInCalls.single().checkedInAt
            assertTrue(checkedInAt != null)
            assertEquals(today, CheckIn("ci", "c1", checkedInAt!!).localDate())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun successfulCheckInReloadKeepsFullHistoryAndProjectsNextDayOccurrence() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val previousDate = today.minus(DatePeriod(days = 1))
            val previousCheckIn = CheckIn("ci-previous", "c1", "${previousDate}T12:00:00Z")
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
                checkIns = listOf(previousCheckIn),
                recentCheckIns = emptyList(),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()

            val updatedContact = viewModel.state.value.contacts.single { it.id == "c1" }
            assertEquals(today.toString(), updatedContact.lastCheckInDate)
            assertEquals(today.plus(DatePeriod(days = 1)).toString(), updatedContact.nextCheckInDate)
            assertTrue(viewModel.state.value.checkIns.any { it.id == previousCheckIn.id })
            assertTrue(viewModel.state.value.checkIns.any { it.contactId == "c1" && it.checkedInAt.startsWith(today.toString()) })

            val nextDayEvents = buildCheckInTimelineEvents(
                checkIns = viewModel.state.value.checkIns,
                contacts = viewModel.state.value.contacts,
                today = today.plus(DatePeriod(days = 1)),
            )
            assertTrue(nextDayEvents.any { it.date == previousDate && it.checkedIn })
            assertTrue(nextDayEvents.any { it.date == today && it.checkedIn })
            assertTrue(nextDayEvents.any { it.date == today.plus(DatePeriod(days = 1)) && !it.checkedIn })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refreshedHistoryKeepsEarlierAvatarWhenCheckInResponseOmitsAvatarColor() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val previousDate = today.minus(DatePeriod(days = 1))
            val dataSource = FakeContactDataSource(
                contacts = listOf(
                    contact("first", next = today.toString()),
                    contact("second", next = today.toString()).copy(avatarColor = "#FF3B30"),
                ),
                checkIns = listOf(
                    CheckIn("ci-previous", "first", "${previousDate}T12:00:00Z"),
                ),
                recentCheckIns = emptyList(),
            ).apply {
                logCheckInResponseTransform = { updated ->
                    if (updated.id == "first") updated.copy(avatarColor = null) else updated
                }
            }
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("first")
            advanceUntilIdle()
            viewModel.checkIn("second")
            advanceUntilIdle()

            val slots = buildTimelineSlots(
                startDate = timelineStartForToday(today),
                today = today,
                events = buildCheckInTimelineEvents(
                    checkIns = viewModel.state.value.checkIns,
                    contacts = viewModel.state.value.contacts,
                    today = today,
                ),
            )
            val previousSlot = slots.single { it.date == previousDate }

            assertEquals(1, previousSlot.avatarCount)
            assertEquals(1, timelineRenderedAvatarCount(previousSlot))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun initialCountdownAnchorRemainsInHomeStateAfterSuccessfulCheckInReload() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("first", next = today.toString())),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(today, viewModel.state.value.initialCountdownStartDate)

            viewModel.checkIn("first")
            advanceUntilIdle()

            val nextDay = today.plus(DatePeriod(days = 1))
            val slots = buildTimelineSlots(
                startDate = timelineStartForToday(
                    today = nextDay,
                    initialCountdownStartDate = viewModel.state.value.initialCountdownStartDate,
                ),
                today = nextDay,
                events = buildCheckInTimelineEvents(
                    checkIns = viewModel.state.value.checkIns,
                    contacts = viewModel.state.value.contacts,
                    today = nextDay,
                ),
            )

            assertEquals(today, slots.first().date)
            assertTrue(slots.first().isCheckedIn)
            assertEquals(nextDay, slots[1].date)
            assertTrue(slots[1].hasPendingCheckIn)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedCheckInClearsLoadingAndExposesRetryableError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1"))).apply {
                logCheckInError = ContactError.Unknown("log_check_in is unavailable")
            }
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()

            assertTrue(viewModel.state.value.checkingInContactIds.isEmpty())
            assertNotNull(viewModel.state.value.checkInError)
            assertEquals(1, dataSource.logCheckInCalls.size)
            assertEquals(1, viewModel.state.value.outstandingCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun todayCheckInEventDoesNotRemainOutstandingWhenContactCacheIsStale() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
                checkIns = listOf(CheckIn("ci1", "c1", "${today}T12:00:00Z")),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(0, viewModel.state.value.outstandingCount)
            assertEquals(1, viewModel.state.value.upToDateCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun timelineKeepsAnEarlierCompletedCheckpointAfterALaterCheckIn() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val previousDate = today.minus(DatePeriod(days = 1))
            val contact = contact("c1", next = today.plus(DatePeriod(days = 1)).toString())
                .copy(lastCheckInDate = today.toString())
            val previousCheckIn = CheckIn("ci-previous", "c1", "${previousDate}T12:00:00Z")
            val currentCheckIn = CheckIn("ci-current", "c1", "${today}T12:00:00Z")
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact),
                checkIns = listOf(previousCheckIn, currentCheckIn),
                recentCheckIns = listOf(currentCheckIn),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            val events = buildCheckInTimelineEvents(
                checkIns = viewModel.state.value.checkIns,
                contacts = viewModel.state.value.contacts,
                today = today,
            )

            assertTrue(events.any { it.date == previousDate && it.checkedIn })
            assertTrue(events.any { it.date == today && it.checkedIn })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onlyOutstandingContactsGetAButtonState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val upToDate = contact("c1", next = today.plus(DatePeriod(days = 7)).toString())
            val outstanding = contact("c2", next = today.minus(DatePeriod(days = 2)).toString())
            val dataSource = FakeContactDataSource(contacts = listOf(upToDate, outstanding))
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(listOf("c1", "c2"), viewModel.state.value.contacts.map { it.id })
            assertEquals(1, viewModel.state.value.outstandingCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun alreadyCheckedInContactCannotBeCheckedInAgain() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(
                    contact("c1", next = today.plus(DatePeriod(days = 1)).toString())
                        .copy(lastCheckInDate = today.toString()),
                ),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()

            assertEquals(0, dataSource.logCheckInCalls.size)
            assertEquals(0, dataSource.checkIns.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun todayCheckInListExcludesContactsDueLaterAndIncludesCompletedToday() {
        val completedToday = contact("completed", next = today.plus(DatePeriod(days = 7)).toString())
            .copy(lastCheckInDate = today.toString())
        val dueToday = contact("due")
        val dueLater = contact("later", next = today.plus(DatePeriod(days = 7)).toString())

        assertEquals(
            listOf("due", "completed"),
            listOf(completedToday, dueToday, dueLater).forTodayCheckInList(today).map { it.id },
        )
    }

    @Test
    fun stateContainsTotalCheckInsPerContact() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1"), contact("c2", next = today.plus(DatePeriod(days = 7)).toString())),
                checkIns = listOf(
                    CheckIn("ci1", "c1", "2026-07-01T12:00:00Z"),
                    CheckIn("ci2", "c1", "2026-07-15T12:00:00Z"),
                    CheckIn("ci3", "c2", "2026-07-20T12:00:00Z"),
                ),
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            assertEquals(mapOf("c1" to 2, "c2" to 1), viewModel.state.value.checkInCounts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun overlappingCheckInsForDifferentContactsAreAllowed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1"), contact("c2", next = today.minus(DatePeriod(days = 2)).toString())),
            )
            dataSource.checkInGate = CompletableDeferred()
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)

            // A different contact must be allowed to check in while c1 is pending.
            viewModel.checkIn("c2")
            advanceUntilIdle()
            assertEquals(2, dataSource.logCheckInCalls.size)

            dataSource.checkInGate?.complete(Unit)
            advanceUntilIdle()
            assertEquals(2, dataSource.logCheckInCalls.size)
            assertTrue(viewModel.state.value.checkingInContactIds.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun duplicateTapOfSameContactIsBlockedWhilePending() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1")))
            dataSource.checkInGate = CompletableDeferred()
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)

            viewModel.checkIn("c1")
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)

            dataSource.checkInGate?.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, dataSource.logCheckInCalls.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun successfulCheckInInvalidatesAccountBadgeCache() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1")),
            )
            val homeRepository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "account-a" },
                scope = this,
            )
            val accountRepository = InMemoryAccountRepository(
                profileDataSource = FakeProfileDataSource(),
                contactDataSource = dataSource,
                accountKeyProvider = { "account-a" },
                scope = this,
            )
            accountRepository.load()
            val viewModel = HomeViewModel(
                dataSource,
                ReminderScheduler(),
                homeRepository,
                accountRepository,
            )
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()
            val badgeReadsBeforeAccountRefresh = dataSource.getBadgesCalls
            accountRepository.load()
            advanceUntilIdle()

            assertEquals(badgeReadsBeforeAccountRefresh + 1, dataSource.getBadgesCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun checkInStateRemainsUnchangedBeforeNetworkCompletes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1")))
            dataSource.checkInGate = CompletableDeferred()
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()

            // While the network is still gated, only the loading state is reflected.
            assertTrue(viewModel.state.value.checkingInContactIds.contains("c1"))
            assertEquals(1, viewModel.state.value.outstandingCount)
            assertEquals(0, viewModel.state.value.upToDateCount)
            assertEquals(null, viewModel.state.value.contacts.single { it.id == "c1" }.lastCheckInDate)
            assertTrue(viewModel.state.value.checkIns.isEmpty())

            dataSource.checkInGate?.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.checkingInContactIds.isEmpty())
            assertEquals(today.toString(), viewModel.state.value.contacts.single { it.id == "c1" }.lastCheckInDate)
            assertEquals(1, viewModel.state.value.checkIns.size)
            assertTrue(viewModel.state.value.checkIns.single().id.startsWith("temp-"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedCheckInRollsBackExactly() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(
                contacts = listOf(contact("c1"), contact("c2", next = today.minus(DatePeriod(days = 2)).toString())),
                checkIns = emptyList(),
            )
            dataSource.logCheckInError = ContactError.Unknown("boom")
            val viewModel = HomeViewModel(dataSource, ReminderScheduler())
            advanceUntilIdle()

            val beforeCounts = viewModel.state.value.outstandingCount
            val beforeCheckIns = viewModel.state.value.checkIns

            viewModel.checkIn("c1")
            advanceUntilIdle()

            // Exact pre-tap state must be restored: no residue from the optimistic patch.
            assertEquals(beforeCounts, viewModel.state.value.outstandingCount)
            assertEquals(beforeCheckIns, viewModel.state.value.checkIns)
            assertEquals(null, viewModel.state.value.contacts.single { it.id == "c1" }.lastCheckInDate)
            assertTrue(viewModel.state.value.checkingInContactIds.isEmpty())
            assertTrue(viewModel.state.value.checkInError != null)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reconciliationReplacesTempCheckInWithoutDuplicating() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dataSource = FakeContactDataSource(contacts = listOf(contact("c1")))
            val homeRepository = InMemoryHomeRepository(
                contactDataSource = dataSource,
                accountKeyProvider = { "account-a" },
                scope = this,
            )
            val viewModel = HomeViewModel(dataSource, ReminderScheduler(), homeRepository)
            advanceUntilIdle()

            viewModel.checkIn("c1")
            advanceUntilIdle()
            assertEquals(1, viewModel.state.value.checkIns.size)
            assertTrue(viewModel.state.value.checkIns.single().id.startsWith("temp-"))

            // A background reconciliation reload now swaps the temp entry for the
            // server row — never leaving both in the list.
            viewModel.loadContacts(forceRefresh = true)
            advanceUntilIdle()

            val reloaded = viewModel.state.value.checkIns
            assertEquals(1, reloaded.size)
            assertTrue(reloaded.none { it.id.startsWith("temp-") })
        } finally {
            Dispatchers.resetMain()
        }
    }
}
