package app.usenekko.navigation

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
    }

    @Test
    fun navigateAndGoBackUpdateCurrentScreen() {
        val navigator = Navigator(Screen.Welcome)

        navigator.navigate(Screen.Contact)

        assertEquals(Screen.Contact, navigator.currentScreen)
        assertTrue(navigator.goBack())
        assertEquals(Screen.Welcome, navigator.currentScreen)
    }

    @Test
    fun paywallNavigationCanReturnToPreviousScreen() {
        val navigator = Navigator(Screen.Home)

        navigator.navigate(Screen.Paywall)

        assertEquals(Screen.Paywall, navigator.currentScreen)
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
    }
}
