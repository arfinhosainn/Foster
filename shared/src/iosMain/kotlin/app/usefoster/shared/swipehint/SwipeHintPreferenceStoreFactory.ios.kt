package app.usefoster.shared.swipehint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private class NSUserDefaultsSwipeHintPreferenceDataSource : SwipeHintPreferenceDataSource {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val seenHintKey = "has_seen_swipe_hint"
    private val swipedBookKey = "has_swiped_home_book"

    override suspend fun hasSeenHint(): Boolean =
        defaults.boolForKey(seenHintKey)

    override suspend fun setHasSeenHint(seen: Boolean) {
        defaults.setBool(seen, forKey = seenHintKey)
        defaults.synchronize()
    }

    override suspend fun hasSwipedBook(): Boolean =
        defaults.boolForKey(swipedBookKey)

    override suspend fun setHasSwipedBook(swiped: Boolean) {
        defaults.setBool(swiped, forKey = swipedBookKey)
        defaults.synchronize()
    }
}

@Composable
actual fun rememberSwipeHintStore(): SwipeHintPreferenceStore =
    remember { SwipeHintPreferenceStore(NSUserDefaultsSwipeHintPreferenceDataSource()) }