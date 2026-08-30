package app.usefoster.home

import app.usefoster.home.data.InMemoryAccountRepository
import app.usefoster.home.domain.Badge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import app.usefoster.home.domain.UserBadge
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryTest {

    @Test
    fun warmLoadReturnsCachedProfileAndBadgesWithoutReads() = runTest {
        val profileDataSource = FakeProfileDataSource()
        val contactDataSource = dataSource()
        val repository = repository(profileDataSource, contactDataSource, this)

        repository.load()
        val profileCalls = profileDataSource.getProfileCalls
        contactDataSource.resetCounts()
        val result = repository.load()

        assertEquals("Jane Bell", (result as app.usefoster.shared.domain.Result.Success).data.profile?.resolvedName)
        assertEquals(profileCalls, profileDataSource.getProfileCalls)
        assertEquals(0, contactDataSource.getBadgesCalls)
        assertEquals(0, contactDataSource.getUserBadgesCalls)
        assertFalse(repository.state.value.isRefreshing)
    }

    @Test
    fun staleLoadReturnsOldSnapshotThenPublishesFreshProfile() = runTest {
        var now = Instant.parse("2026-08-14T12:00:00Z")
        val profileDataSource = FakeProfileDataSource()
        val contactDataSource = dataSource()
        val repository = repository(profileDataSource, contactDataSource, this, now = { now })

        repository.load()
        profileDataSource.profile = profileDataSource.profile!!.copy(fullName = "Updated Name")
        now = Instant.parse("2026-08-14T12:00:31Z")
        val cached = repository.load()

        assertEquals("Jane Bell", (cached as app.usefoster.shared.domain.Result.Success).data.profile?.resolvedName)
        assertTrue(repository.state.value.isRefreshing)
        advanceUntilIdle()

        assertEquals("Updated Name", repository.state.value.snapshot?.profile?.resolvedName)
        assertFalse(repository.state.value.isRefreshing)
    }

    @Test
    fun concurrentRefreshesShareOneBadgeBatch() = runTest {
        val profileDataSource = FakeProfileDataSource().apply {
            getProfileGate = CompletableDeferred()
        }
        val contactDataSource = dataSource()
        val repository = repository(profileDataSource, contactDataSource, this)

        val first = async { repository.load(forceRefresh = true) }
        advanceUntilIdle()
        val second = async { repository.load(forceRefresh = true) }
        advanceUntilIdle()

        assertEquals(1, profileDataSource.getProfileCalls)
        assertEquals(1, contactDataSource.getBadgesCalls)
        assertEquals(1, contactDataSource.getUserBadgesCalls)

        profileDataSource.getProfileGate?.complete(Unit)
        advanceUntilIdle()
        assertNotNull(first.await())
        assertNotNull(second.await())
    }

    @Test
    fun failedRefreshRetainsUsableSnapshot() = runTest {
        val profileDataSource = FakeProfileDataSource()
        val contactDataSource = dataSource()
        val repository = repository(profileDataSource, contactDataSource, this)

        repository.load()
        val previous = repository.state.value.snapshot
        contactDataSource.badgesError = app.usefoster.home.domain.ContactError.Network
        repository.invalidate()
        val result = repository.load(forceRefresh = true)

        assertTrue(result is app.usefoster.shared.domain.Result.Error)
        assertEquals(previous, repository.state.value.snapshot)
        assertFalse(repository.state.value.isRefreshing)
        assertNotNull(repository.state.value.error)
    }

    @Test
    fun changingAccountNeverReturnsPreviousSnapshot() = runTest {
        var accountKey = "account-a"
        val profileDataSource = FakeProfileDataSource()
        val contactDataSource = dataSource()
        val repository = repository(
            profileDataSource,
            contactDataSource,
            this,
            accountKey = { accountKey },
        )

        repository.load()
        accountKey = "account-b"
        profileDataSource.profile = profileDataSource.profile!!.copy(fullName = "Second User")
        val result = repository.load()

        assertEquals("Second User", (result as app.usefoster.shared.domain.Result.Success).data.profile?.resolvedName)
        assertEquals("account-b", repository.state.value.snapshot?.accountKey)
    }

    private fun dataSource() = FakeContactDataSource(
        badges = listOf(Badge("b1", "Green", "green", 1)),
        userBadges = listOf(UserBadge("b1", "2026-08-14T12:00:00Z")),
    )

    private fun repository(
        profileDataSource: FakeProfileDataSource,
        contactDataSource: FakeContactDataSource,
        scope: kotlinx.coroutines.CoroutineScope,
        now: () -> Instant = { Instant.parse("2026-08-14T12:00:00Z") },
        accountKey: suspend () -> String? = { "account-a" },
    ) = InMemoryAccountRepository(
        profileDataSource = profileDataSource,
        contactDataSource = contactDataSource,
        accountKeyProvider = accountKey,
        scope = scope,
        now = now,
    )
}