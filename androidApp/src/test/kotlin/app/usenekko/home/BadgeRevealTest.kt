package app.usenekko.home

import app.usenekko.home.domain.Badge
import app.usenekko.home.domain.UserBadge
import app.usenekko.home.presentation.badges.BadgeRevealStore
import app.usenekko.home.presentation.badges.detectAndTriggerBadgeReveal
import app.usenekko.home.presentation.badges.newlyUnlockedBadge
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeRevealTest {

    private fun badge(id: String, threshold: Int) = Badge(id, "b$id", "desc $id", threshold)

    @Test
    fun newlyUnlockedPicksLowestThresholdBadge() {
        val badges = listOf(
            badge("a", 15),
            badge("b", 1),
            badge("c", 150),
        )
        val result = newlyUnlockedBadge(
            previousIds = emptySet(),
            afterIds = setOf("a", "b"),
            badges = badges,
        )
        assertEquals("b", result?.id)
    }

    @Test
    fun newlyUnlockedReturnsNullWhenNothingNew() {
        val badges = listOf(badge("a", 1))
        val result = newlyUnlockedBadge(
            previousIds = setOf("a"),
            afterIds = setOf("a"),
            badges = badges,
        )
        assertNull(result)
    }

    @Test
    fun newlyUnlockedIgnoresPreviouslyHeldBadges() {
        val badges = listOf(badge("a", 1), badge("b", 15))
        val result = newlyUnlockedBadge(
            previousIds = setOf("a"),
            afterIds = setOf("a", "b"),
            badges = badges,
        )
        assertEquals("b", result?.id)
    }

    @Test
    fun detectTriggersRevealWhenBadgeAppears() = runTest {
        val dataSource = FakeContactDataSource(
            badges = listOf(badge("b1", 1), badge("b2", 15)),
            userBadges = listOf(UserBadge("b2", "2026-08-04T10:00:00Z")),
        )
        try {
            dataSource.detectAndTriggerBadgeReveal(previousBadgeIds = emptySet())
            assertEquals("b2", BadgeRevealStore.pending.value?.id)
        } finally {
            BadgeRevealStore.consume()
        }
    }

    @Test
    fun detectDoesNotTriggerWhenNoNewBadge() = runTest {
        val dataSource = FakeContactDataSource(
            badges = listOf(badge("b1", 1)),
            userBadges = listOf(UserBadge("b1", "2026-08-04T10:00:00Z")),
        )
        try {
            dataSource.detectAndTriggerBadgeReveal(previousBadgeIds = setOf("b1"))
            assertNull(BadgeRevealStore.pending.value)
        } finally {
            BadgeRevealStore.consume()
        }
    }
}
