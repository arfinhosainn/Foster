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

        navigator.navigate(Screen.Email)

        assertEquals(Screen.Email, navigator.currentScreen)
        assertTrue(navigator.goBack())
        assertEquals(Screen.Welcome, navigator.currentScreen)
    }

    @Test
    fun replaceAllClearsPreviousStack() {
        val navigator = Navigator(Screen.Welcome)

        navigator.navigate(Screen.Email)
        navigator.replaceAll(Screen.Name)

        assertEquals(Screen.Name, navigator.currentScreen)
        assertEquals(listOf(Screen.Name), navigator.backStack.toList())
        assertFalse(navigator.canGoBack)
    }
}
