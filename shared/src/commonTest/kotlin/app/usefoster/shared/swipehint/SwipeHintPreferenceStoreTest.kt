package app.usefoster.shared.swipehint

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeHintPreferenceStoreTest {

    private class FakeDataSource(
        initialSeen: Boolean = false,
        initialSwiped: Boolean = false,
    ) : SwipeHintPreferenceDataSource {
        var seen: Boolean = initialSeen
        var swiped: Boolean = initialSwiped
        var loaded: Boolean = false

        override suspend fun hasSeenHint(): Boolean {
            loaded = true
            return seen
        }

        override suspend fun setHasSeenHint(seen: Boolean) {
            this.seen = seen
        }

        override suspend fun hasSwipedBook(): Boolean = swiped

        override suspend fun setHasSwipedBook(swiped: Boolean) {
            this.swiped = swiped
        }
    }

    private suspend fun awaitInit(dataSource: FakeDataSource) {
        withTimeout(3_000) {
            while (!dataSource.loaded) delay(5)
        }
    }

    private suspend fun awaitValue(current: () -> Boolean, expected: Boolean) {
        withTimeout(3_000) {
            while (current() != expected) delay(5)
        }
    }

    @Test
    fun newUserStartsWithBothFlagsFalse() = runBlocking {
        val dataSource = FakeDataSource()
        val store = SwipeHintPreferenceStore(dataSource)

        awaitInit(dataSource)
        assertFalse(store.hasSeenHint.value)
        assertFalse(store.hasSwipedBook.value)
    }

    @Test
    fun hintShownLoadsPersistedFlagAcrossRestarts() = runBlocking {
        // Session 1: hint plays -> hasSeenHint persisted.
        val first = FakeDataSource()
        val store1 = SwipeHintPreferenceStore(first)
        awaitInit(first)

        store1.markHintShown()
        awaitValue({ first.seen }, expected = true)
        assertTrue(store1.hasSeenHint.value)

        // Session 2 (same device): the flag is loaded, so the hint is retired.
        val second = FakeDataSource(initialSeen = true)
        val store2 = SwipeHintPreferenceStore(second)
        awaitInit(second)
        assertTrue(store2.hasSeenHint.value)
    }

    @Test
    fun aSwipePersistsAndRetiresTheHintEvenBeforeItPlays() = runBlocking {
        val dataSource = FakeDataSource()
        val store = SwipeHintPreferenceStore(dataSource)
        awaitInit(dataSource)

        store.markSwiped()
        awaitValue({ dataSource.swiped }, expected = true)
        assertTrue(store.hasSwipedBook.value)
        assertFalse(store.hasSeenHint.value)

        // Next launch: swiped flag loaded -> hint must not show again.
        val next = FakeDataSource(initialSwiped = true)
        val storeNext = SwipeHintPreferenceStore(next)
        awaitInit(next)
        assertTrue(storeNext.hasSwipedBook.value)
    }

    @Test
    fun hintMayOnlyPlayWhileBothFlagsAreFalse() = runBlocking {
        val freshSource = FakeDataSource()
        val fresh = SwipeHintPreferenceStore(freshSource)
        awaitInit(freshSource)
        assertFalse(fresh.hasSeenHint.value || fresh.hasSwipedBook.value)

        val seenSource = FakeDataSource(initialSeen = true)
        val seen = SwipeHintPreferenceStore(seenSource)
        awaitInit(seenSource)
        assertTrue(seen.hasSeenHint.value)

        val swipedSource = FakeDataSource(initialSwiped = true)
        val swiped = SwipeHintPreferenceStore(swipedSource)
        awaitInit(swipedSource)
        assertTrue(swiped.hasSwipedBook.value)

        val bothSource = FakeDataSource(initialSeen = true, initialSwiped = true)
        val both = SwipeHintPreferenceStore(bothSource)
        awaitInit(bothSource)
        assertTrue(both.hasSeenHint.value && both.hasSwipedBook.value)
    }
}