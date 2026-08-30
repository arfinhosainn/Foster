package app.usefoster.home

import app.usefoster.home.data.InMemoryBrainstormRepository
import app.usefoster.home.domain.BrainstormError
import app.usefoster.home.domain.BrainstormSession
import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrainstormRepositoryTest {

    @Test
    fun coldLoadPublishesHistoryAndWarmLoadAvoidsNetworkRead() = runTest {
        val dataSource = FakeBrainstormDataSource().apply { history = listOf(session("first")) }
        val repository = repository(dataSource, this)

        repository.load("contact-a")
        val calls = dataSource.getHistoryCalls
        val result = repository.load("contact-a")

        assertEquals(1, calls)
        assertEquals(listOf("first"), titles(result))
        assertFalse(repository.state("contact-a").value.isRefreshing)
    }

    @Test
    fun staleLoadReturnsCachedHistoryThenPublishesRefresh() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        val dataSource = FakeBrainstormDataSource().apply { history = listOf(session("old")) }
        val repository = repository(dataSource, this, now = { now })
        repository.load("contact-a")

        dataSource.history = listOf(session("new"))
        dataSource.historyGate = CompletableDeferred()
        now = Instant.parse("2026-08-14T12:00:31Z")
        val cached = repository.load("contact-a")

        assertEquals(listOf("old"), titles(cached))
        assertTrue(repository.state("contact-a").value.isRefreshing)
        dataSource.historyGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("new"), repository.state("contact-a").value.snapshot?.history?.map { it.topics.first().title })
        assertEquals(2, dataSource.getHistoryCalls)
    }

    @Test
    fun concurrentRefreshesUseOneHistoryRequest() = runTest {
        val gate = CompletableDeferred<Unit>()
        val dataSource = FakeBrainstormDataSource().apply { historyGate = gate }
        val repository = repository(dataSource, this)

        val first = async { repository.load("contact-a", forceRefresh = true) }
        advanceUntilIdle()
        val second = async { repository.load("contact-a", forceRefresh = true) }
        advanceUntilIdle()

        assertEquals(1, dataSource.getHistoryCalls)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(first.await() is Result.Success)
        assertTrue(second.await() is Result.Success)
    }

    @Test
    fun refreshFailureRetainsCachedHistory() = runTest {
        val dataSource = FakeBrainstormDataSource().apply { history = listOf(session("saved")) }
        val repository = repository(dataSource, this)
        repository.load("contact-a")
        dataSource.historyError = BrainstormError.Network
        repository.invalidate("contact-a")

        val result = repository.load("contact-a", forceRefresh = true)

        assertTrue(result is Result.Error)
        assertEquals(listOf("saved"), repository.state("contact-a").value.snapshot?.history?.map { it.topics.first().title })
        assertFalse(repository.state("contact-a").value.isRefreshing)
        assertEquals(BrainstormError.Network, repository.state("contact-a").value.error)
    }

    @Test
    fun invalidationRefreshesHistoryWithoutReturningEmptyState() = runTest {
        val dataSource = FakeBrainstormDataSource().apply { history = listOf(session("before")) }
        val repository = repository(dataSource, this)
        repository.load("contact-a")
        dataSource.history = listOf(session("after"))
        repository.invalidate("contact-a")

        val cached = repository.load("contact-a")
        assertEquals(listOf("before"), titles(cached))
        advanceUntilIdle()
        assertEquals(listOf("after"), repository.state("contact-a").value.snapshot?.history?.map { it.topics.first().title })
    }

    @Test
    fun accountChangeClearsPreviousContactHistory() = runTest {
        var accountKey = "account-a"
        val dataSource = FakeBrainstormDataSource().apply { history = listOf(session("first user")) }
        val repository = repository(dataSource, this, accountKey = { accountKey })
        repository.load("contact-a")

        accountKey = "account-b"
        dataSource.history = listOf(session("second user"))
        repository.load("contact-a")

        val state = repository.state("contact-a").value
        assertEquals("account-b", state.snapshot?.accountKey)
        assertEquals(listOf("second user"), state.snapshot?.history?.map { it.topics.first().title })
    }

    @Test
    fun generationInvalidatesCachedHistory() = runTest {
        val dataSource = FakeBrainstormDataSource().apply {
            history = listOf(session("before"))
            generateResult = Result.Success(
                app.usefoster.home.domain.BrainstormGeneration.Generated(listOf(topic("generated"))),
            )
        }
        val repository = repository(dataSource, this)
        repository.load("contact-a")
        dataSource.history = listOf(session("after"))

        repository.generate("contact-a")
        repository.load("contact-a")
        advanceUntilIdle()

        assertEquals(listOf("after"), repository.state("contact-a").value.snapshot?.history?.map { it.topics.first().title })
    }

    private fun repository(
        dataSource: FakeBrainstormDataSource,
        scope: kotlinx.coroutines.CoroutineScope,
        now: () -> Instant = { Instant.parse("2026-08-14T12:00:00Z") },
        accountKey: () -> String? = { "account-a" },
    ) = InMemoryBrainstormRepository(
        dataSource = dataSource,
        accountKeyProvider = accountKey,
        scope = scope,
        now = now,
    )

    private fun session(title: String) = BrainstormSession(
        id = title,
        createdAt = "2026-08-14T12:00:00Z",
        topics = listOf(topic(title)),
    )

    private fun topic(title: String) = BrainstormTopic(id = title, title = title)

    private fun titles(result: Result<app.usefoster.home.data.BrainstormHistorySnapshot, BrainstormError>) =
        (result as Result.Success).data.history.map { it.topics.first().title }
}