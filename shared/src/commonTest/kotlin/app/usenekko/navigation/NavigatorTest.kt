package app.usenekko.navigation

import androidx.compose.runtime.mutableStateListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {
    @Test
    fun goBackDoesNotRemoveStartDestination() {
        val navigator = Navigator(Screen.Welcome)

        assertFalse(navigator.goBack())

        assertEquals(Screen.Welcome, navigator.currentScreen)
        assertEquals(listOf(Screen.Welcome), navigator.backStack.toList())
        assertEquals(
            NavState(Screen.Welcome, NavigationOperation.ResetStack, depth = 1, zIndex = 0),
            navigator.navState,
        )
    }

    @Test
    fun restoredStackStartsWithNeutralResetState() {
        val navigator = Navigator(
            mutableStateListOf(Screen.Welcome, Screen.Contact),
        )

        assertEquals(Screen.Contact, navigator.currentScreen)
        assertEquals(NavigationOperation.ResetStack, navigator.navState.operation)
        assertEquals(2, navigator.navState.depth)
        assertEquals(0, navigator.navState.zIndex)
    }

    @Test
    fun navigateAndGoBackUpdateCurrentScreen() {
        val navigator = Navigator(Screen.Welcome)

        navigator.navigate(Screen.Contact)

        assertEquals(Screen.Contact, navigator.currentScreen)
        assertEquals(NavigationOperation.Forward, navigator.navState.operation)
        assertEquals(2, navigator.navState.depth)
        assertEquals(1, navigator.navState.zIndex)

        assertTrue(navigator.goBack())
        assertEquals(Screen.Welcome, navigator.currentScreen)
        assertEquals(NavigationOperation.Backward, navigator.navState.operation)
        assertEquals(1, navigator.navState.depth)
        assertEquals(0, navigator.navState.zIndex)
    }

    @Test
    fun paywallNavigationCanReturnToPreviousScreen() {
        val navigator = Navigator(Screen.Home)

        navigator.navigate(Screen.Paywall)

        assertEquals(Screen.Paywall, navigator.currentScreen)
        assertTrue(navigator.goBack())
        assertEquals(Screen.Home, navigator.currentScreen)
        assertEquals(NavigationOperation.Backward, navigator.navState.operation)
    }

    @Test
    fun checkInsNavigationCanReturnHome() {
        val navigator = Navigator(Screen.Home)

        navigator.navigate(Screen.CheckIns)

        assertEquals(Screen.CheckIns, navigator.currentScreen)
        assertTrue(navigator.goBack())
        assertEquals(Screen.Home, navigator.currentScreen)
    }

    @Test
    fun replaceAllClearsPreviousStack() {
        val navigator = Navigator(Screen.Welcome)

        navigator.navigate(Screen.Contact)
        navigator.replaceAll(Screen.Name)

        assertEquals(Screen.Name, navigator.currentScreen)
        assertEquals(listOf(Screen.Name), navigator.backStack.toList())
        assertFalse(navigator.canGoBack)
        assertEquals(NavigationOperation.ResetStack, navigator.navState.operation)
        assertEquals(1, navigator.navState.depth)
        assertEquals(2, navigator.navState.zIndex)
    }

    @Test
    fun replaceUsesTargetDirectedOperationAndPreservesDepth() {
        val navigator = Navigator(Screen.Welcome)

        navigator.navigate(Screen.Contact)
        navigator.replace(Screen.Group)

        assertEquals(listOf(Screen.Welcome, Screen.Group), navigator.backStack.toList())
        assertEquals(NavigationOperation.Replace, navigator.navState.operation)
        assertEquals(2, navigator.navState.depth)
        assertEquals(2, navigator.navState.zIndex)
    }

    @Test
    fun replaceAllRaisesZIndexEvenWhenResettingToRoot() {
        val navigator = Navigator(Screen.Welcome)

        navigator.navigate(Screen.Contact)
        navigator.navigate(Screen.Group)
        navigator.replaceAll(Screen.Home)

        assertEquals(NavigationOperation.ResetStack, navigator.navState.operation)
        assertEquals(1, navigator.navState.depth)
        assertEquals(3, navigator.navState.zIndex)
    }
}
