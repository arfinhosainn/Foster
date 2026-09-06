package app.usefoster.shared.swipehint

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Durable flags for the one-time Home swipe hint (mirrors ThemePreferenceStore):
 *
 *  - [hasSeenHint]: set the first time the hint animation actually plays, so it
 *    never replays on later app launches.
 *  - [hasSwipedBook]: set the first time the user really drags the Home/History
 *    pager, so the hint is retired the moment the interaction is understood —
 *    even if it hadn't played yet (e.g. the user swiped during the delay, or on
 *    a previous session before we shipped the animation).
 *
 * Both flags persist via [SwipeHintPreferenceDataSource] (DataStore on Android,
 * NSUserDefaults on iOS). The hint may only play while BOTH are false.
 */
class SwipeHintPreferenceStore(
    private val dataSource: SwipeHintPreferenceDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _hasSeenHint = MutableStateFlow(false)
    val hasSeenHint: StateFlow<Boolean> = _hasSeenHint.asStateFlow()

    private val _hasSwipedBook = MutableStateFlow(false)
    val hasSwipedBook: StateFlow<Boolean> = _hasSwipedBook.asStateFlow()

    init {
        scope.launch {
            _hasSeenHint.value = dataSource.hasSeenHint()
            _hasSwipedBook.value = dataSource.hasSwipedBook()
        }
    }

    fun markHintShown() {
        _hasSeenHint.value = true
        scope.launch {
            dataSource.setHasSeenHint(true)
        }
    }

    fun markSwiped() {
        _hasSwipedBook.value = true
        scope.launch {
            dataSource.setHasSwipedBook(true)
        }
    }
}