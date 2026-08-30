package app.usefoster.home

import app.usefoster.home.domain.BrainstormGeneration
import app.usefoster.home.domain.BrainstormSession
import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.home.data.InMemoryBrainstormRepository
import app.usefoster.home.presentation.brainstorm.BrainstormViewModel
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the exact logic the Brainstorm UI renders from: on load, Current
 * Output shows the newest session's topics; after a successful generate it is
 * REPLACED with the freshly generated (personalized) batch — never the old
 * fallback that was loaded from History.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrainstormViewModelTest {

    private fun topic(title: String, desc: String = "desc of $title") =
        BrainstormTopic(id = title, title = title, description = desc)

    private fun session(id: String, createdAt: String, title: String) =
        BrainstormSession(id, createdAt, listOf(topic(title)))

    @Test
    fun historyStartsInLoadingStateBeforeTheFirstSnapshotArrives() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val vm = BrainstormViewModel(
                contactId = "c1",
                repository = repository(FakeBrainstormDataSource(), this),
            )

            assertEquals(true, vm.state.value.isLoadingHistory)

            advanceUntilIdle()

            assertEquals(false, vm.state.value.isLoadingHistory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun generationStartsAutomaticallyWhenViewModelOpens() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val fake = FakeBrainstormDataSource().apply {
                generateResult = Result.Success(
                    BrainstormGeneration.Generated(listOf(topic("Automatic idea"))),
                )
            }

            BrainstormViewModel("c1", repository(fake, this))
            advanceUntilIdle()

            assertEquals(listOf("c1"), fake.generateCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun freeUserCanGenerateWhenMonthlyLimitIsReachedWhilePaywallIsTemporarilyDisabled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val fake = FakeBrainstormDataSource().apply {
                monthlyGenerationCount = 3
                generateResult = Result.Success(
                    BrainstormGeneration.Generated(listOf(topic("Available idea"))),
                )
            }

            val vm = BrainstormViewModel("c1", repository(fake, this))
            advanceUntilIdle()

            assertEquals(listOf("c1"), fake.generateCalls)
            assertEquals(listOf("Available idea"), vm.state.value.currentTopics?.map { it.title })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun currentOutputPopulatedFromNewestSessionOnLoad() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val fake = FakeBrainstormDataSource().apply {
                history = listOf(
                    session("s1", "2026-08-04T11:05:00Z", "Catch up"),
                    session("s0", "2026-08-03T09:00:00Z", "Older"),
                )
            }
            val vm = BrainstormViewModel("c1", repository(fake, this))
            advanceUntilIdle()

            assertEquals(listOf("Catch up"), vm.state.value.currentTopics?.map { it.title })
            assertEquals(2, vm.state.value.history.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun automaticGenerationReplacesStaleFallbackWithNewPersonalizedBatch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            // Start with a fallback session in History (the "old catch-up" the
            // user keeps seeing).
            val fake = FakeBrainstormDataSource().apply {
                history = listOf(
                    session("s1", "2026-08-04T09:00:00Z", "Catch up"),
                    session("s1b", "2026-08-04T09:00:01Z", "How are they doing"),
                )
                // Server returns the personalized batch.
                generateResult = Result.Success(
                    BrainstormGeneration.Generated(listOf(topic("Anniversary plans")))
                )
            }
            val vm = BrainstormViewModel("c1", repository(fake, this))
            advanceUntilIdle()

            // Opening the screen generates automatically and shows the new
            // personalized batch, never the old history fallback.
            assertEquals(listOf("Anniversary plans"), vm.state.value.currentTopics?.map { it.title })
            assertEquals(listOf("c1"), fake.generateCalls)
            assertNull(vm.state.value.notice)
            assertFalse(vm.state.value.isGenerating)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun generateCooldownKeepsTopicsButShowsClearNotice() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val fake = FakeBrainstormDataSource().apply {
                history = listOf(session("s1", "2026-08-04T09:00:00Z", "Old topic"))
                generateResult = Result.Success(BrainstormGeneration.Cooldown)
            }
            val vm = BrainstormViewModel("c1", repository(fake, this))
            advanceUntilIdle()

            assertEquals(listOf("Old topic"), vm.state.value.currentTopics?.map { it.title })
            assertNotNull(vm.state.value.notice)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun repository(
        dataSource: FakeBrainstormDataSource,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = InMemoryBrainstormRepository(
        dataSource = dataSource,
        accountKeyProvider = { "account-a" },
        scope = scope,
    )
}
