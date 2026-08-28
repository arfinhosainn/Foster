package app.usenekko.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Bridge between the persistent bottom navigation bar (owned by the app shell,
 * outside the screen transition animation) and whichever tab screen is currently
 * composed underneath it.
 *
 * Add-contact requests are delivered as a monotonic event count rather than a
 * stored callback: AnimatedContent keeps two screens briefly alive during
 * transitions, and a callback slot would let the outgoing screen's dispose-time
 * cleanup wipe the incoming screen's registration (leaving a dead "+").
 * Screens observe [addContactRequestCount] via `snapshotFlow { }.drop(1)` for
 * exactly as long as they are alive.
 */
class BottomBarActions {

    /** Mirror of the active tab screen's overlay state (add-contact sheet etc.) */
    var isOverlayShowing: Boolean by mutableStateOf(false)

    /** Increments by one per "+" tap; observed by the active tab screen. */
    var addContactRequestCount: Int by mutableIntStateOf(0)
        private set

    fun notifyAddContactRequested() {
        addContactRequestCount++
    }
}
