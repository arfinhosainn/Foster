package app.usefoster.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationTransitionPolicyTest {
    @Test
    fun everyHorizontalRouteUsesHorizontalStyle() {
        val routes = listOf(
            Screen.Welcome,
            Screen.Name,
            Screen.Contact,
            Screen.Group,
            Screen.Reminder,
            Screen.TimeReminder,
            Screen.CustomReminder,
            Screen.AddNote,
            Screen.Notification,
            Screen.ContactProfile("contact-id"),
            Screen.Settings(),
            Screen.GroupDetail("group-id", "Group"),
        )

        routes.forEach { route ->
            assertEquals(
                ScreenTransitionStyle.Horizontal,
                transitionStyle(
                    initial = state(Screen.Home),
                    target = state(route, NavigationOperation.Forward),
                ),
            )
        }
    }

    @Test
    fun everyVerticalRouteUsesVerticalStyle() {
        val routes = listOf(
            Screen.Brainstorm("contact-id"),
            Screen.Paywall,
            Screen.GroupSettings,
        )

        routes.forEach { route ->
            assertEquals(
                ScreenTransitionStyle.Vertical,
                transitionStyle(
                    initial = state(Screen.Home),
                    target = state(route, NavigationOperation.Forward),
                ),
            )
        }
    }

    @Test
    fun homeUsesResetStyleWhenItIsTheForwardTarget() {
        assertEquals(
            ScreenTransitionStyle.Reset,
            transitionStyle(
                initial = state(Screen.Notification),
                target = state(Screen.Home, NavigationOperation.Forward),
            ),
        )
    }

    @Test
    fun backwardUsesOutgoingScreenStyle() {
        assertEquals(
            ScreenTransitionStyle.Vertical,
            transitionStyle(
                initial = state(Screen.Paywall, NavigationOperation.Forward),
                target = state(Screen.Home, NavigationOperation.Backward),
            ),
        )
        assertEquals(
            ScreenTransitionStyle.Horizontal,
            transitionStyle(
                initial = state(Screen.Settings(), NavigationOperation.Forward),
                target = state(Screen.Home, NavigationOperation.Backward),
            ),
        )
    }

    @Test
    fun replaceUsesTargetStyleAndResetAlwaysUsesResetStyle() {
        assertEquals(
            ScreenTransitionStyle.Vertical,
            transitionStyle(
                initial = state(Screen.Home),
                target = state(Screen.GroupSettings, NavigationOperation.Replace),
            ),
        )
        assertEquals(
            ScreenTransitionStyle.Reset,
            transitionStyle(
                initial = state(Screen.Notification),
                target = state(Screen.GroupSettings, NavigationOperation.ResetStack),
            ),
        )
    }

    @Test
    fun inPlacePanePresentationDoesNotReplayRouteAnimation() {
        assertEquals(
            ScreenTransitionStyle.None,
            transitionStyle(
                initial = state(Screen.Home),
                target = state(Screen.ContactProfile("contact-id"), NavigationOperation.Replace),
                presentation = NavigationPresentation.InPlacePane,
            ),
        )
    }

    @Test
    fun animationSpecsAreCentralized() {
        assertEquals(300, NavAnimationSpecs.HorizontalDurationMillis)
        assertEquals(400, NavAnimationSpecs.VerticalDurationMillis)
        assertEquals(200, NavAnimationSpecs.ResetDurationMillis)
        assertEquals(0.92f, NavAnimationSpecs.ResetInitialScale)
        assertEquals(0.3f, NavAnimationSpecs.ForwardParallaxFraction)
    }

    @Test
    fun historyIsAPagerPageWithNoRouteAnimation() {
        // Home and History are two pages of one pager-backed book — the pager
        // owns the motion, so AnimatedContent must stay out of the way for the
        // pair in BOTH directions.
        assertEquals(
            ScreenTransitionStyle.None,
            transitionStyle(
                initial = state(Screen.Home),
                target = state(Screen.CheckInHistory, NavigationOperation.Forward),
            ),
        )
        assertEquals(
            ScreenTransitionStyle.None,
            transitionStyle(
                initial = state(Screen.CheckInHistory),
                target = state(Screen.Home, NavigationOperation.Backward),
            ),
        )
    }

    @Test
    fun homeAndHistoryShareOneAnimatedContentSlot() {
        // Same slot = the pager is never torn down, double-composed, or
        // re-animated when focus flips between the two book pages.
        assertEquals(navContentKey(Screen.Home), navContentKey(Screen.CheckInHistory))
        kotlin.test.assertNotEquals(navContentKey(Screen.Home), navContentKey(Screen.CheckIns))
        kotlin.test.assertNotEquals(
            navContentKey(Screen.Home),
            navContentKey(Screen.Settings()),
        )
    }

    private fun state(
        screen: Screen,
        operation: NavigationOperation = NavigationOperation.ResetStack,
    ) = NavState(screen, operation, depth = 1, zIndex = 0)
}