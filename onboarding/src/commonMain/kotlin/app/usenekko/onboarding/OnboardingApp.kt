package app.usenekko.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.usenekko.App
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.addnote.AddNoteScreen
import app.usenekko.onboarding.contact.ContactScreen
import app.usenekko.onboarding.customreminder.CustomReminderScreen
import app.usenekko.onboarding.dayreminder.ReminderScreen
import app.usenekko.onboarding.group.GroupScreen
import app.usenekko.onboarding.name.NameScreen
import app.usenekko.onboarding.notification.NotificationScreen
import app.usenekko.onboarding.presentation.LocalOnboardingProfileDataSource
import app.usenekko.onboarding.presentation.LocalSupabaseClient
import app.usenekko.onboarding.presentation.OnboardingDraftStoreProvider
import app.usenekko.onboarding.data.supabase.SupabaseOnboardingProfileDataSource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import app.usenekko.onboarding.timereminder.TimeReminderScreen
import app.usenekko.onboarding.welcome.WelcomeScreen
import app.usenekko.onboarding.splash.SplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.usenekko.adaptive.WindowWidthSizeClass
import app.usenekko.adaptive.windowWidthSizeClass
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.GlassBottomNavBar
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.GlassNavigationRail
import app.usenekko.navigation.BottomBarActions
import app.usenekko.home.HomeScreen
import app.usenekko.home.presentation.dayagenda.DayAgendaScreen
import app.usenekko.home.presentation.CheckInsScreen
import app.usenekko.home.presentation.badges.BadgeRevealStore
import app.usenekko.home.presentation.badges.PlantRewardOverlay
import app.usenekko.home.presentation.brainstorm.BrainstormScreen
import app.usenekko.home.presentation.contactprofile.ContactProfileScreen
import app.usenekko.home.presentation.paywall.DiscountPaywallScreen
import app.usenekko.home.presentation.paywall.PaywallScreen
import app.usenekko.home.presentation.settings.AccountScreen
import app.usenekko.home.presentation.settings.GroupDetailScreen
import app.usenekko.home.presentation.settings.GroupSettingsScreen
import app.usenekko.home.presentation.settings.SettingScreen
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.shared.domain.Result
import app.usenekko.shared.paywall.LocalPaywallGateManager
import app.usenekko.shared.paywall.PaywallGateManagerProvider
import app.usenekko.shared.paywall.PaywallTrigger
import app.usenekko.shared.subscription.LocalSubscriptionRepository
import app.usenekko.theme.NekkoTheme
import app.usenekko.theme.ThemePreferenceStoreProvider
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient

@Composable
fun OnboardingApp(navigator: Navigator, supabaseClient: SupabaseClient? = null) {
    OnboardingDraftStoreProvider(supabaseClient) {
        // Sits below OnboardingDraftStoreProvider (needs LocalSubscriptionRepository).
        PaywallGateManagerProvider {
            OnboardingAppContent(navigator, supabaseClient)
        }
    }
}

@Composable
private fun OnboardingAppContent(navigator: Navigator, supabaseClient: SupabaseClient?) {
    val profileDataSource = LocalOnboardingProfileDataSource.current
    val supabaseClient = LocalSupabaseClient.current
    val scope = rememberCoroutineScope()

    val subscriptionRepository = LocalSubscriptionRepository.current
    val paywallGateManager = LocalPaywallGateManager.current

    LaunchedEffect(Unit) {
        var recoveryAttempted = false
        launch {
            subscriptionRepository.refresh()
        }
        supabaseClient.auth.sessionStatus.collect { status ->
            when (authSessionAction(status, navigator.currentScreen is Screen.Splash)) {
                AuthSessionAction.Route -> {
                    recoveryAttempted = false
                    val session = supabaseClient.auth.currentSessionOrNull()
                    logAccount(session?.user?.email, session?.user?.id, "authenticated session")
                    routeAfterAuth(profileDataSource, navigator)
                }
                AuthSessionAction.Recover -> {
                    if (!recoveryAttempted) {
                        recoveryAttempted = true
                        runCatching { supabaseClient.auth.refreshCurrentSession() }
                            .onFailure { error ->
                                println("NekkoAuth[session refresh failed]: ${error.message}")
                            }
                    }
                }
                AuthSessionAction.ShowWelcome -> navigator.replaceAll(Screen.Welcome)
                AuthSessionAction.Ignore -> Unit
            }
        }
    }

    // Single delivery point for every gate-approved impression (exit-intent,
    // abandoned checkout, limit hit, aha moment, win-back): whenever the engine
    // approves an offer, route to the discount paywall once. Deliberately waits
    // until the initial auth routing has settled (Splash -> real screen):
    // navigating earlier would get wiped out by routeAfterAuth's replaceAll.
    val shouldShowDiscountPaywall by paywallGateManager.shouldShowDiscountPaywall.collectAsState()
    val currentScreen = navigator.currentScreen
    LaunchedEffect(shouldShowDiscountPaywall, currentScreen) {
        if (!shouldShowDiscountPaywall) return@LaunchedEffect
        if (currentScreen is Screen.Splash || currentScreen is Screen.DiscountPaywall) return@LaunchedEffect
        paywallGateManager.consumeShow()
        navigator.navigate(Screen.DiscountPaywall)
    }

    // Explicit premium entry points (home crown button, settings upgrade card,
    // limit-hit fallback, ...): while the discounted offer's countdown is still
    // running, EVERY premium surface leads to the discount paywall; once it has
    // expired (or never started), fall back to the regular paywall.
    val showPremiumPaywall: () -> Unit = {
        scope.launch {
            if (paywallGateManager.isDiscountOfferLive()) {
                paywallGateManager.onDiscountPaywallShown()
                navigator.navigate(Screen.DiscountPaywall)
            } else {
                navigator.navigate(Screen.Paywall)
            }
        }
    }

    val pendingBadge by BadgeRevealStore.pending.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRailLayout = windowWidthSizeClass(maxWidth) == WindowWidthSizeClass.Expanded
        val bottomBarActions = remember { BottomBarActions() }

        // Screens animate inside this padded container; the floating bottom bar /
        // rail lives OUTSIDE it so its selection circle never participates in
        // (or jumps during) screen transitions.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useRailLayout) Modifier.padding(start = 88.dp) else Modifier),
        ) {
        App(navigator) { screen ->
            val screenContent: @Composable () -> Unit = {
                when (screen) {
                    is Screen.Splash -> SplashScreen()

                    is Screen.Welcome -> WelcomeScreen(
                        supabaseClient = supabaseClient,
                        onGoogleSignInSuccess = {
                            val session = supabaseClient.auth.currentSessionOrNull()
                            logAccount(session?.user?.email, session?.user?.id, "Google sign-in")
                            scope.launch { routeAfterAuth(profileDataSource, navigator) }
                        },
                        onAppleSignInSuccess = {
                            val session = supabaseClient.auth.currentSessionOrNull()
                            logAccount(session?.user?.email, session?.user?.id, "Apple sign-in")
                            scope.launch { routeAfterAuth(profileDataSource, navigator) }
                        },
                    )

                    is Screen.Name -> NameScreen(
                        onNavigateToNext = { navigator.navigate(Screen.Contact) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.Contact) },
                    )

                    is Screen.Contact -> ContactScreen(
                        onNavigateToNext = { navigator.navigate(Screen.Group) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.Group) },
                    )

                    is Screen.Group -> GroupScreen(
                        onNavigateToNext = { navigator.navigate(Screen.Reminder) },
                        onBack = { navigator.goBack() },
                    )

                    is Screen.Reminder -> ReminderScreen(
                        onNavigateToNext = { navigator.navigate(Screen.TimeReminder) },
                        onBack = { navigator.goBack() },
                    )

                    is Screen.TimeReminder -> TimeReminderScreen(
                        onNavigateToNext = { navigator.navigate(Screen.CustomReminder) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.CustomReminder) },
                    )

                    is Screen.CustomReminder -> CustomReminderScreen(
                        onNavigateToNext = { navigator.navigate(Screen.AddNote) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.AddNote) },
                    )

                    is Screen.AddNote -> AddNoteScreen(
                        onNavigateToNext = { navigator.navigate(Screen.Notification) },
                        onBack = { navigator.goBack() },
                        onSkip = { navigator.navigate(Screen.Notification) },
                    )

                    is Screen.Notification -> NotificationScreen(
                        onNavigateToMainApp = {
                            // Land on Home, then show the paywall on top of it:
                            // closing the paywall (or subscribing) returns the
                            // user to the home screen.
                            navigator.replaceAll(Screen.Home)
                            if (!subscriptionRepository.isSubscribed.value) {
                                navigator.navigate(Screen.Paywall)
                            }
                        },
                        onBack = { navigator.goBack() },
                    )

                    is Screen.Home -> HomeScreen(
                        onContactClick = { contact -> navigator.navigate(Screen.ContactProfile(contact.id)) },
                        onBrainstormClick = { contactId -> navigator.navigate(Screen.Brainstorm(contactId)) },
                        onCheckInsClick = { navigator.navigate(Screen.CheckIns) },
                        onSettingsClick = { navigator.navigate(Screen.Settings) },
                        onShowPaywall = showPremiumPaywall,
                        onShowDiscountPaywall = { navigator.navigate(Screen.DiscountPaywall) },
                        bottomBarActions = bottomBarActions,
                    )

                    is Screen.CheckIns -> CheckInsScreen(
                        // Horizontal pop back to Home so both tab directions use
                        // matching directional page-style slides (no Reset flash).
                        onHomeClick = { navigator.goBack() },
                        onSettingsClick = { navigator.navigate(Screen.Settings) },
                        onShowPaywall = showPremiumPaywall,
                        onShowDiscountPaywall = { navigator.navigate(Screen.DiscountPaywall) },
                        bottomBarActions = bottomBarActions,
                    )

                    is Screen.ContactProfile -> ContactProfileScreen(
                        contactId = screen.contactId,
                        onBack = { navigator.goBack() },
                        onBrainstormClick = { navigator.navigate(Screen.Brainstorm(screen.contactId)) },
                    )

                is Screen.Brainstorm -> BrainstormScreen(
                    contactId = screen.contactId,
                )

                    is Screen.Settings -> SettingScreen(
                        onBack = { navigator.goBack() },
                        onAccountClick = {},
                        onPremiumClick = showPremiumPaywall,
                        onAccountDeleted = {
                            scope.launch {
                                // The server row is already gone (the Edge Function
                                // returned success). Best-effort local sign-out so the
                                // stale session doesn't leave the app half-authenticated,
                                // then drop the whole stack back to Welcome (no account).
                                runCatching { supabaseClient.auth.signOut() }
                                navigator.replaceAll(Screen.Welcome)
                            }
                        },
                    )

                    is Screen.Account -> AccountScreen(
                        onBack = { navigator.goBack() },
                    )

                    is Screen.GroupSettings -> GroupSettingsScreen(
                        onBack = { navigator.goBack() },
                    )

                    is Screen.GroupDetail -> GroupDetailScreen(
                        groupId = screen.groupId,
                        onBack = { navigator.goBack() },
                    )

                    is Screen.Paywall -> PaywallScreen(
                        onBack = {
                            // Closing the regular paywall arms exit-intent and
                            // offers the 60% deal IMMEDIATELY in this session;
                            // if the gates block it here, the armed dismissal
                            // still fires on the next cold start as a fallback.
                            paywallGateManager.onRegularPaywallDismissed()
                            navigator.goBack()
                            scope.launch {
                                paywallGateManager.reportTrigger(PaywallTrigger.EXIT_INTENT)
                            }
                        },
                        onSubscribed = { navigator.goBack() },
                    )

                is Screen.DiscountPaywall -> DiscountPaywallScreen(
                    onSubscribed = { navigator.goBack() },
                )

                is Screen.DayAgenda -> DayAgendaScreen(
                    dayKey = screen.dayKey,
                    onBack = { navigator.goBack() },
                    onContactClick = { contactId ->
                        navigator.navigate(Screen.ContactProfile(contactId))
                    },
                )
            }
            }
            if (screen.isFirstRunSurface) {
                NekkoTheme(darkTheme = true) { screenContent() }
            } else {
                screenContent()
            }
        }
        } // end rail-padded content container

        // Single persistent navigation bar: its selection circle glides between
        // tabs while screens slide underneath (see NavigationTransitionPolicy).
        PersistentBottomNavigationBar(
            navigator = navigator,
            actions = bottomBarActions,
            useRail = useRailLayout,
            onSelectTab = { index -> navigator.selectMainTab(index) },
            modifier = Modifier.align(if (useRailLayout) Alignment.CenterStart else Alignment.BottomCenter),
        )

        pendingBadge?.let { badge ->
            NekkoTheme {
                PlantRewardOverlay(
                    badge = badge,
                    onCollect = {
                        BadgeRevealStore.consume()
                        navigator.navigate(Screen.Account)
                    },
                    onDismiss = { BadgeRevealStore.consume() },
                )
            }
        }
    }
}

internal enum class AuthSessionAction {
    Route,
    Recover,
    ShowWelcome,
    Ignore,
}

val Screen.isFirstRunSurface: Boolean
    get() = when (this) {
        Screen.Splash,
        Screen.Welcome,
        Screen.Name,
        Screen.Contact,
        Screen.Group,
        Screen.Reminder,
        Screen.TimeReminder,
        Screen.CustomReminder,
        Screen.AddNote,
        Screen.Notification,
        -> true

        else -> false
    }

/**
 * Tab selection for the shell-owned bottom bar. Tap targets:
 * - Tab 0 (Home): from CheckIns, pop back with a leftward slide.
 * - Tab 1 (Grow): from Home, push forward with a rightward slide.
 */
private fun Navigator.selectMainTab(index: Int) {
    when (index) {
        0 -> if (currentScreen is Screen.CheckIns) goBack()
        1 -> if (currentScreen is Screen.Home) navigate(Screen.CheckIns)
    }
}

/**
 * The one bottom bar shared by all tab screens. Rendered above [app.usenekko.App]'s
 * AnimatedContent so the indicator's travel animation runs concurrently with —
 * and unaffected by — the horizontal page transition of the screens. Hidden
 * while non-tab screens are pushed or while a tab screen shows an overlay.
 */
@Composable
private fun PersistentBottomNavigationBar(
    navigator: Navigator,
    actions: BottomBarActions,
    useRail: Boolean,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The shell renders this bar outside App()'s NekkoTheme (that theme only
    // wraps the NavHost), so supply the same wrapper stack locally — otherwise
    // GlassBottomNavBar reads missing ExtendedColors and crashes on first show.
    ThemePreferenceStoreProvider {
    NekkoTheme {
    val selectedIndex: Int? = when (navigator.currentScreen) {
        Screen.Home -> 0
        Screen.CheckIns -> 1
        else -> null
    }

    // Freeze the last tab while non-tab destinations sit on top, so the fading
    // bar doesn't visibly reset its circle back to item 0 mid-exit.
    val stickySelectedIndex = remember { mutableIntStateOf(0) }
    if (selectedIndex != null && !actions.isOverlayShowing) {
        stickySelectedIndex.intValue = selectedIndex
    }

    val visible = selectedIndex != null && !actions.isOverlayShowing

    AnimatedVisibility(
        visible = visible,
        enter =
            if (useRail) fadeIn(tween(200))
            else slideInVertically(tween(240)) { it } + fadeIn(tween(240)),
        exit =
            if (useRail) fadeOut(tween(160))
            else slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
        modifier = modifier,
    ) {
        if (useRail) {
            GlassNavigationRail(
                selectedIndex = stickySelectedIndex.intValue,
                onItemSelected = onSelectTab,
                onAddClick = actions::notifyAddContactRequested,
            )
        } else {
            GlassBottomNavBar(
                selectedIndex = stickySelectedIndex.intValue,
                onItemSelected = onSelectTab,
                onAddClick = actions::notifyAddContactRequested,
            )
        }
    }
    }
    }
} // end NekkoTheme / ThemePreferenceStoreProvider / bar composable

internal fun authSessionAction(status: SessionStatus, isSplash: Boolean): AuthSessionAction {
    if (!isSplash) return AuthSessionAction.Ignore
    return when (status) {
        is SessionStatus.Authenticated -> AuthSessionAction.Route
        is SessionStatus.RefreshFailure -> AuthSessionAction.Recover
        is SessionStatus.NotAuthenticated -> AuthSessionAction.ShowWelcome
        SessionStatus.Initializing,
        -> AuthSessionAction.Ignore
    }
}

private suspend fun routeAfterAuth(
    profileDataSource: OnboardingProfileDataSource,
    navigator: Navigator,
) {
    when (val stepResult = profileDataSource.getOnboardingStep()) {
        is Result.Success -> {
            navigator.replaceAll(stepResult.data.toScreen())
        }
        is Result.Error -> {
            profileDataSource.ensureProfileExists()
            navigator.replaceAll(Screen.Name)
        }
    }
}

private fun OnboardingStep?.toScreen(): Screen {
    return when (this) {
        null,
        OnboardingStep.Welcome -> Screen.Welcome
        OnboardingStep.Name -> Screen.Name
        OnboardingStep.Contact -> Screen.Contact
        OnboardingStep.Group -> Screen.Group
        OnboardingStep.DayReminder -> Screen.Reminder
        OnboardingStep.TimeReminder -> Screen.TimeReminder
        OnboardingStep.CustomReminder -> Screen.CustomReminder
        OnboardingStep.AddNote -> Screen.AddNote
        OnboardingStep.Notification -> Screen.Notification
        OnboardingStep.Complete -> Screen.Home
    }
}

private fun logAccount(email: String?, userId: String?, source: String) {
    kotlin.io.println("NekkoAuth[$source]: email=${email ?: "null"} userId=${userId ?: "null"}")
}
