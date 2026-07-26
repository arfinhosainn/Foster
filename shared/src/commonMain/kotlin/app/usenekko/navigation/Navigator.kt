package app.usenekko.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator internal constructor(
    val backStack: SnapshotStateList<Screen>
) {
    constructor(startDestination: Screen) : this(mutableStateListOf(startDestination))

    val currentScreen: Screen
        get() = backStack.last()

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun replace(screen: Screen) {
        if (backStack.isEmpty()) {
            backStack.add(screen)
        } else {
            backStack[backStack.lastIndex] = screen
        }
    }

    fun replaceAll(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun goBack(): Boolean {
        if (!canGoBack) return false
        backStack.removeLastOrNull()
        return true
    }
}
