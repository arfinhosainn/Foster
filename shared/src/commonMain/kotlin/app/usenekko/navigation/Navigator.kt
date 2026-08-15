package app.usenekko.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList

enum class NavigationOperation {
    Forward,
    Backward,
    Replace,
    ResetStack,
}

data class NavState(
    val screen: Screen,
    val operation: NavigationOperation,
    val depth: Int,
    val zIndex: Int,
)

class Navigator internal constructor(
    val backStack: SnapshotStateList<Screen>
) {
    constructor(startDestination: Screen) : this(mutableStateListOf(startDestination))

    private val navStateValue = mutableStateOf(
        NavState(
            screen = backStack.last(),
            operation = NavigationOperation.ResetStack,
            depth = backStack.size,
            zIndex = 0,
        )
    )

    val navState: NavState
        get() = navStateValue.value

    val currentScreen: Screen
        get() = navState.screen

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigate(screen: Screen) {
        updateNavigation(NavigationOperation.Forward, navState.zIndex + 1) {
            backStack.add(screen)
        }
    }

    fun replace(screen: Screen) {
        updateNavigation(NavigationOperation.Replace, navState.zIndex + 1) {
            if (backStack.isEmpty()) {
                backStack.add(screen)
            } else {
                backStack[backStack.lastIndex] = screen
            }
        }
    }

    fun replaceAll(screen: Screen) {
        updateNavigation(NavigationOperation.ResetStack, navState.zIndex + 1) {
            backStack.clear()
            backStack.add(screen)
        }
    }

    fun goBack(): Boolean {
        if (!canGoBack) return false
        updateNavigation(NavigationOperation.Backward, navState.zIndex - 1) {
            backStack.removeLastOrNull()
        }
        return true
    }

    private fun updateNavigation(
        operation: NavigationOperation,
        zIndex: Int,
        mutation: () -> Unit,
    ) {
        Snapshot.withMutableSnapshot {
            mutation()
            navStateValue.value = NavState(
                screen = backStack.last(),
                operation = operation,
                depth = backStack.size,
                zIndex = zIndex,
            )
        }
    }
}
