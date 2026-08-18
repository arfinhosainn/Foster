package app.usenekko.navigation

sealed class ScreenTransitionStyle {
    data object Horizontal : ScreenTransitionStyle()

    data object Vertical : ScreenTransitionStyle()

    data object Reset : ScreenTransitionStyle()

    data object None : ScreenTransitionStyle()
}

enum class NavigationPresentation {
    FullScreen,
    InPlacePane,
}

object NavAnimationSpecs {
    const val HorizontalDurationMillis = 300
    const val VerticalDurationMillis = 400
    const val ResetDurationMillis = 200
    const val ResetInitialScale = 0.92f
    const val ForwardParallaxFraction = 0.3f
}

fun transitionStyle(
    initial: NavState,
    target: NavState,
    presentation: NavigationPresentation = NavigationPresentation.FullScreen,
): ScreenTransitionStyle {
    if (presentation == NavigationPresentation.InPlacePane) {
        return ScreenTransitionStyle.None
    }

    return when (target.operation) {
        NavigationOperation.Backward -> screenTransitionStyle(initial.screen)
        NavigationOperation.Forward,
        NavigationOperation.Replace -> screenTransitionStyle(target.screen)
        NavigationOperation.ResetStack -> ScreenTransitionStyle.Reset
    }
}

private fun screenTransitionStyle(screen: Screen): ScreenTransitionStyle {
    return when (screen) {
        Screen.Splash -> ScreenTransitionStyle.None
        Screen.Welcome,
        Screen.Name,
        Screen.Contact,
        Screen.Group,
        Screen.Reminder,
        Screen.TimeReminder,
        Screen.CustomReminder,
        Screen.AddNote,
        Screen.Notification,
        is Screen.ContactProfile,
        Screen.Settings,
        Screen.CheckIns,
        is Screen.GroupDetail -> ScreenTransitionStyle.Horizontal

        Screen.Home -> ScreenTransitionStyle.Reset
        is Screen.Brainstorm,
        Screen.Paywall,
        Screen.Account,
        Screen.GroupSettings -> ScreenTransitionStyle.Vertical
    }
}