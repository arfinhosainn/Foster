package app.usefoster.home

import app.usefoster.home.presentation.history.HISTORY_BOOK_HISTORY_PAGE
import app.usefoster.home.presentation.history.HISTORY_BOOK_HOME_PAGE
import app.usefoster.home.presentation.history.HomeBookAction
import app.usefoster.home.presentation.history.homeBookActionFor
import app.usefoster.home.presentation.history.historyBookTargetPage
import app.usefoster.home.presentation.history.historyIsPoppedOnReturn
import app.usefoster.navigation.Navigator
import app.usefoster.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Home/History book's settle→route policy: this is where swipe/navigation
 * regressions hide, so the cases cover echoes, interrupted gestures, repeated
 * round trips and the History-as-root edge — not just the happy path.
 */
class HomeHistoryBookPolicyTest {

    @Test
    fun settlingOnHistoryFromHomePushesHistory() {
        assertEquals(
            HomeBookAction.OpenHistory,
            homeBookActionFor(HISTORY_BOOK_HISTORY_PAGE, Screen.Home),
        )
    }

    @Test
    fun settlingOnHomeFromHistoryReturnsHome() {
        assertEquals(
            HomeBookAction.ReturnToHome,
            homeBookActionFor(HISTORY_BOOK_HOME_PAGE, Screen.CheckInHistory),
        )
    }

    @Test
    fun settleEchoesFromProgrammaticScrollsAreNoOps() {
        // Tap the status card: route becomes History, then the settle event
        // lands — the route already agrees, so nothing dispatches (no loop).
        assertNull(homeBookActionFor(HISTORY_BOOK_HISTORY_PAGE, Screen.CheckInHistory))
        // Tap the top-bar back: route becomes Home first.
        assertNull(homeBookActionFor(HISTORY_BOOK_HOME_PAGE, Screen.Home))
    }

    @Test
    fun targetPageMirrorsTheRoute() {
        assertEquals(HISTORY_BOOK_HISTORY_PAGE, historyBookTargetPage(Screen.CheckInHistory))
        assertEquals(HISTORY_BOOK_HOME_PAGE, historyBookTargetPage(Screen.Home))
        // Any other screen is not part of the book — the book is gone by then,
        // but the fallback must still resolve to the Home page.
        assertEquals(HISTORY_BOOK_HOME_PAGE, historyBookTargetPage(Screen.CheckIns))
    }

    @Test
    fun repeatedRoundTripsDoNotGrowTheBackStack() {
        val navigator = Navigator(Screen.Home)

        fun settle(page: Int) {
            when (homeBookActionFor(page, navigator.currentScreen)) {
                HomeBookAction.OpenHistory -> navigator.navigate(Screen.CheckInHistory)
                HomeBookAction.ReturnToHome ->
                    if (historyIsPoppedOnReturn(navigator.backStack)) {
                        navigator.goBack()
                    } else {
                        navigator.replace(Screen.Home)
                    }

                null -> Unit
            }
        }

        repeat(3) {
            // Drag Home -> History.
            settle(HISTORY_BOOK_HISTORY_PAGE)
            assertEquals(2, navigator.backStack.size)
            assertEquals(Screen.CheckInHistory, navigator.currentScreen)

            // Drag History -> Home.
            settle(HISTORY_BOOK_HOME_PAGE)
            assertEquals(1, navigator.backStack.size)
            assertEquals(Screen.Home, navigator.currentScreen)
        }
    }

    @Test
    fun interruptedProgrammaticOpenConvergesOnNextSettle() {
        val navigator = Navigator(Screen.Home)

        // User taps the status card: route pushes History before the pager
        // finishes animating there.
        navigator.navigate(Screen.CheckInHistory)
        assertEquals(Screen.CheckInHistory, navigator.currentScreen)

        // ...then grabs the pager mid-animation and drags back to Home. The
        // settle must reconcile the route instead of fighting the animation.
        assertEquals(
            HomeBookAction.ReturnToHome,
            homeBookActionFor(HISTORY_BOOK_HOME_PAGE, navigator.currentScreen),
        )
        if (historyIsPoppedOnReturn(navigator.backStack)) navigator.goBack()
        assertEquals(Screen.Home, navigator.currentScreen)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun historyRootedStackIsReplacedWithHomeNotPopped() {
        // Deep link / restore edge: History with no Home beneath it. Popping
        // would strand an empty stack, so the shell replaces it with Home.
        assertEquals(false, historyIsPoppedOnReturn(listOf(Screen.CheckInHistory)))
        assertEquals(true, historyIsPoppedOnReturn(listOf(Screen.Home, Screen.CheckInHistory)))

        val navigator = Navigator(Screen.CheckInHistory)
        val action = homeBookActionFor(HISTORY_BOOK_HOME_PAGE, navigator.currentScreen)
        assertEquals(HomeBookAction.ReturnToHome, action)
        if (historyIsPoppedOnReturn(navigator.backStack)) {
            navigator.goBack()
        } else {
            navigator.replace(Screen.Home)
        }
        assertEquals(listOf(Screen.Home), navigator.backStack.toList())
    }
}