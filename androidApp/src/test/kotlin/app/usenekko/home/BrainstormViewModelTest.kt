package app.usenekko.home

import app.usenekko.home.domain.BrainstormGeneration
import app.usenekko.home.domain.BrainstormSession
import app.usenekko.home.domain.BrainstormTopic
import app.usenekko.home.presentation.brainstorm.BrainstormAction
import app.usenekko.home.presentation.brainstorm.BrainstormViewModel
import app.usenekko.shared.domain.Result
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
            val vm = BrainstormViewModel("c1", fake, FakeSubscriptionRepository())
            advanceUntilIdle()

            assertEquals(listOf("Catch up"), vm.state.value.currentTopics?.map { it.title })
            assertEquals(2, vm.state.value.history.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun generateReplacesStaleFallbackWithNewPersonalizedBatch() = runTest {
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
            val vm = BrainstormViewModel("c1", fake, FakeSubscriptionRepository())
            advanceUntilIdle()
            // Before generating, Current Output reflects the first session in
            // History (the fake returns history in the order supplied).
            assertEquals(listOf("Catch up"), vm.state.value.currentTopics?.map { it.title })

            vm.onAction(BrainstormAction.Generate)
            advanceUntilIdle()

            // The UI must now show the NEW personalized batch, never the old one.
            assertEquals(listOf("Anniversary plans"), vm.state.value.currentTopics?.map { it.title })
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
            val vm = BrainstormViewModel("c1", fake, FakeSubscriptionRepository())
            advanceUntilIdle()

            vm.onAction(BrainstormAction.Generate)
            advanceUntilIdle()

            assertEquals(listOf("Old topic"), vm.state.value.currentTopics?.map { it.title })
            assertNotNull(vm.state.value.notice)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
