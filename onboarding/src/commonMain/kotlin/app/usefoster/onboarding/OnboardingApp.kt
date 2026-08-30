package app.usefoster.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import app.usefoster.App
import app.usefoster.navigation.Navigator
import app.usefoster.navigation.Screen
import app.usefoster.onboarding.addnote.AddNoteScreen
import app.usefoster.onboarding.contact.ContactScreen
import app.usefoster.onboarding.customreminder.CustomReminderScreen
import app.usefoster.onboarding.dayreminder.ReminderScreen
import app.usefoster.onboarding.group.GroupScreen
import app.usefoster.onboarding.name.NameScreen
import app.usefoster.onboarding.notification.NotificationScreen
import app.usefoster.onboarding.presentation.LocalOnboardingProfileDataSource
import app.usefoster.onboarding.presentation.LocalOnboardingDraftStore
import app.usefoster.onboarding.presentation.LocalSupabaseClient
import app.usefoster.onboarding.presentation.OnboardingDraftStoreProvider
import app.usefoster.onboarding.data.supabase.SupabaseOnboardingProfileDataSource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import app.usefoster.onboarding.timereminder.TimeReminderScreen
import app.usefoster.onboarding.welcome.WelcomeScreen
import app.usefoster.onboarding.splash.SplashScreen
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import app.usefoster.adaptive.WindowWidthSizeClass
import app.usefoster.adaptive.windowWidthSizeClass
import app.usefoster.designsystem.navbar.bottom.bottomNavBar.SolidBottomNavBar
import app.usefoster.designsystem.navbar.bottom.bottomNavBar.SolidNavigationRail
import app.usefoster.navigation.BottomBarActions
import app.usefoster.home.HomeScreen
import app.usefoster.home.presentation.dayagenda.DayAgendaScreen
import app.usefoster.home.presentation.CheckInsScreen
import app.usefoster.home.presentation.badges.BadgeRevealStore
import app.usefoster.home.presentation.badges.PlantRewardOverlay
import app.usefoster.home.presentation.brainstorm.BrainstormScreen
import app.usefoster.home.presentation.contactprofile.ContactProfileScreen
import app.usefoster.home.presentation.paywall.DiscountPaywallScreen
import app.usefoster.home.presentation.paywall.PaywallScreen
import app.usefoster.home.presentation.settings.GroupDetailScreen
import app.usefoster.home.presentation.settings.GroupSettingsScreen
import app.usefoster.home.presentation.settings.SettingScreen
import app.usefoster.onboarding.domain.OnboardingProfileDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.shared.domain.Result
import app.usefoster.shared.paywall.LocalPaywallGateManager
import app.usefoster.shared.paywall.PaywallGateManagerProvider
import app.usefoster.shared.paywall.PaywallTrigger
import app.usefoster.shared.subscription.LocalSubscriptionRepository
import app.usefoster.theme.FosterTheme
import app.usefoster.theme.ThemePreferenceStoreProvider
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.error_draft_clear
import foster.onboarding.generated.resources.error_draft_corrupt
import foster.onboarding.generated.resources.error_draft_read
import foster.onboarding.generated.resources.error_draft_write
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OnboardingApp(
    navigator: Navigator,
    supabaseClient: SupabaseClient? = null,
    onSplashBusyChanged: (Boolean) -> Unit = {},
) {
    OnboardingDraftStoreProvider(supabaseClient) {
        // Sits below OnboardingDraftStoreProvider (needs LocalSubscriptionRepository).
        PaywallGateManagerProvider {
            // ONE shared theme store for the whole shell (screens, bottom bar,
            // reward overlay) so the in-app Light/Dark setting applies everywhere —
            // previously the bar/overlay sat outside any provider and fell back
            // to the DEVICE theme.
            ThemePreferenceStoreProvider {
                FosterTheme {
                    OnboardingAppContent(navigator, supabaseClient, onSplashBusyChanged)
                }
            }
        }
    }
}

@Composable
private fun OnboardingAppContent(
    navigator: Navigator,
    supabaseClient: SupabaseClient?,
    onSplashBusyChanged: (Boolean) -> Unit,
) {
    val profileDataSource = LocalOnboardingProfileDataSource.current
    val draftStore = LocalOnboardingDraftStore.current
    val supabaseClient = LocalSupabaseClient.current
    val scope = rememberCoroutineScope()
    val profileLoadError = remember { mutableStateOf<OnboardingProfileError?>(null) }
    val draftStorageSnackbarHostState = remember { SnackbarHostState() }
    val draftStorageMessages = DraftStorageMessages(
        read = stringResource(Res.string.error_draft_read),
        write = stringResource(Res.string.error_draft_write),
        clear = stringResource(Res.string.error_draft_clear),
        corrupt = stringResource(Res.string.error_draft_corrupt),
    )

    LaunchedEffect(draftStore, draftStorageMessages) {
        draftStore.storageErrors.collect { error ->
            draftStorageSnackbarHostState.showSnackbar(draftStorageMessages.message(error))
        }
    }

    val subscriptionRepository = LocalSubscriptionRepository.current
    val paywallGateManager = LocalPaywallGateManager.current

    // Silent startup refresh of the subscription entitlement: this is a
    // background refresh with no user action to retry, so a failure must NOT
    // pop the shared snackbar — previously every cold start where RevenueCat
    // was unconfigured/unreachable surfaced "Something went wrong. Please try
    // again." over the Home screen. The last-known entitlement state is kept,
    // and user-initiated surfaces (paywall, restore) surface their own errors.
    LaunchedEffect(subscriptionRepository) {
        subscriptionRepository.refresh()
    }

    LaunchedEffect(Unit) {
        var recoveryAttempted = false
        supabaseClient.auth.sessionStatus.collect { status ->
            when (authSessionAction(status, navigator.currentScreen is Screen.Splash)) {
                AuthSessionAction.Route -> {
                    recoveryAttempted = false
                    profileLoadError.value = null
                    logAccount("authenticated session")
                    routeAfterAuth(profileDataSource, navigator) { error ->
                        profileLoadError.value = error
                    }
                }
                AuthSessionAction.Recover -> {
                    if (!recoveryAttempted) {
                        recoveryAttempted = true
                        try {
                            supabaseClient.auth.refreshCurrentSession()
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            println("FosterAuth[session refresh failed]")
                            profileLoadError.value = OnboardingProfileError.Network
                        }
                    }
                }
                AuthSessionAction.ShowWelcome -> {
                    profileLoadError.value = null
                    navigator.replaceAll(Screen.Welcome)
                }
                AuthSessionAction.Ignore -> Unit
            }
        }
    }

    // Reports whether the splash is still "busy" (auth/session check running,
    // user still on Screen.Splash with no profile error). Android holds the
    // system splash on screen while this is true; once the check fails the
    // error/retry UI must be released into view. No-op on other targets.
    LaunchedEffect(navigator.currentScreen, profileLoadError.value) {
        onSplashBusyChanged(
            navigator.currentScreen is Screen.Splash && profileLoadError.value == null,
        )
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
    val showRewardOverlay = pendingBadge != null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRailLayout = windowWidthSizeClass(maxWidth) == WindowWidthSizeClass.Expanded
        val bottomBarActions = remember { BottomBarActions() }

        // The rail is only visible on the two tab destinations (Home / CheckIns);
        // on every other screen — including all of onboarding — it is hidden.
        // Reserve the 88dp gutter only when the rail is actually shown, so tablet
        // onboarding content isn't indented next to an empty strip.
        val selectedIndex: Int? = when (navigator.currentScreen) {
            Screen.Home -> 0
            Screen.CheckIns -> 1
            else -> null
        }
        val railVisible = useRailLayout && selectedIndex != null

        // While the reward overlay is up, everything behind it is blurred.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showRewardOverlay) Modifier.blur(20.dp) else Modifier),
        ) {

        // Screens animate inside this padded container; the floating bottom bar /
        // rail lives OUTSIDE it so its selection circle never participates in
        // (or jumps during) screen transitions.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (railVisible) Modifier.padding(start = 88.dp) else Modifier),
        ) {
        App(navigator) { screen ->
            val screenContent: @Composable () -> Unit = {
                when (screen) {
                    is Screen.Splash -> SplashScreen(
                        profileLoadError = profileLoadError.value,
                        onRetry = {
                            profileLoadError.value = null
                            scope.launch {
                                if (supabaseClient.auth.currentSessionOrNull() == null) {
                                    navigator.replaceAll(Screen.Welcome)
                                } else {
                                    routeAfterAuth(profileDataSource, navigator) { error ->
                                        profileLoadError.value = error
                                    }
                                }
                            }
                        },
                    )

                    is Screen.Welcome -> WelcomeScreen(
                        supabaseClient = supabaseClient,
                        profileLoadError = profileLoadError.value,
                        onRetryProfileLoad = {
                            profileLoadError.value = null
                            scope.launch {
                                routeAfterSignIn(
                                    supabaseClient = supabaseClient,
                                    profileDataSource = profileDataSource,
                                    navigator = navigator,
                                    source = "profile retry",
                                ) { error ->
                                    profileLoadError.value = error
                                }
                            }
                        },
                        onGoogleSignInSuccess = {
                            profileLoadError.value = null
                            scope.launch {
                                routeAfterSignIn(
                                    supabaseClient = supabaseClient,
                                    profileDataSource = profileDataSource,
                                    navigator = navigator,
                                    source = "Google sign-in",
                                ) { error ->
                                    profileLoadError.value = error
                                }
                            }
                        },
                        onAppleSignInSuccess = {
                            profileLoadError.value = null
                            scope.launch {
                                routeAfterSignIn(
                                    supabaseClient = supabaseClient,
                                    profileDataSource = profileDataSource,
                                    navigator = navigator,
                                    source = "Apple sign-in",
                                ) { error ->
                                    profileLoadError.value = error
                                }
                            }
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
                        onSettingsClick = { navigator.navigate(Screen.Settings()) },
                        onShowPaywall = showPremiumPaywall,
                        onShowDiscountPaywall = { navigator.navigate(Screen.DiscountPaywall) },
                        bottomBarActions = bottomBarActions,
                    )

                    is Screen.CheckIns -> CheckInsScreen(
                        // Horizontal pop back to Home so both tab directions use
                        // matching directional page-style slides (no Reset flash).
                        onHomeClick = { navigator.goBack() },
                        onSettingsClick = { navigator.navigate(Screen.Settings()) },
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
                                try {
                                    supabaseClient.auth.signOut()
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    println("FosterAuth[sign out after account deletion failed]")
                                }
                                navigator.replaceAll(Screen.Welcome)
                            }
                        },
                        initiallyShowAccountSheet = screen.openAccountSheet,
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
                FosterTheme(darkTheme = true) { screenContent() }
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
        } // end blurred backdrop container

        pendingBadge?.let { badge ->
            PlantRewardOverlay(
                badge = badge,
                onCollect = {
                    BadgeRevealStore.consume()
                    // Land on Settings with the account sheet pre-opened — that's
                    // the real profile surface (the standalone Account screen is
                    // no longer part of this flow).
                    navigator.navigate(Screen.Settings(openAccountSheet = true))
                },
                onDismiss = { BadgeRevealStore.consume() },
            )
        }

        SnackbarHost(
            hostState = draftStorageSnackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

internal enum class AuthSessionAction {
    Route,
    Recover,
    ShowWelcome,
    Ignore,
}

private data class DraftStorageMessages(
    val read: String,
    val write: String,
    val clear: String,
    val corrupt: String,
) {
    fun message(error: OnboardingDraftStorageError): String = when (error) {
        OnboardingDraftStorageError.Read -> read
        OnboardingDraftStorageError.Write -> write
        OnboardingDraftStorageError.Clear -> clear
        OnboardingDraftStorageError.Corrupt -> corrupt
    }
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
 * The one bottom bar shared by all tab screens. Rendered above [app.usefoster.App]'s
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
    // Theme now comes from the single root wrapper in OnboardingApp, so this bar
    // follows the in-app Light/Dark setting live (it used to sit outside any
    // provider and read a stale snapshot / device theme).
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
            SolidNavigationRail(
                selectedIndex = stickySelectedIndex.intValue,
                onItemSelected = onSelectTab,
                onAddClick = actions::notifyAddContactRequested,
            )
        } else {
            SolidBottomNavBar(
                selectedIndex = stickySelectedIndex.intValue,
                onItemSelected = onSelectTab,
                onAddClick = actions::notifyAddContactRequested,
            )
        }
    }
} // end PersistentBottomNavigationBar

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

internal suspend fun routeAfterAuth(
    profileDataSource: OnboardingProfileDataSource,
    navigator: Navigator,
    onProfileLoadError: (OnboardingProfileError) -> Unit = {},
) {
    println("FosterAuth[profile routing]: loading onboarding state")
    when (val stepResult = profileDataSource.getOnboardingStep()) {
        is Result.Success -> {
            println("FosterAuth[profile routing]: onboarding step=${stepResult.data}")
            navigator.replaceAll(stepResult.data.toScreen())
        }
        is Result.Error -> {
            println("FosterAuth[profile routing]: profile load failed")
            if (stepResult.error == OnboardingProfileError.ProfileNotFound) {
                when (val ensureResult = profileDataSource.ensureProfileExists()) {
                    is Result.Success -> {
                        println("FosterAuth[profile routing]: profile created; opening onboarding")
                        navigator.replaceAll(Screen.Name)
                    }
                    is Result.Error -> {
                        println("FosterAuth[profile routing]: profile creation failed")
                        onProfileLoadError(ensureResult.error)
                    }
                }
            } else {
                onProfileLoadError(stepResult.error)
            }
        }
    }
}

private suspend fun routeAfterSignIn(
    supabaseClient: SupabaseClient,
    profileDataSource: OnboardingProfileDataSource,
    navigator: Navigator,
    source: String,
    onProfileLoadError: (OnboardingProfileError) -> Unit,
) {
    if (supabaseClient.auth.currentSessionOrNull() == null) {
        val authenticated = withTimeoutOrNull(10_000.milliseconds) {
            supabaseClient.auth.sessionStatus.first { status ->
                status is SessionStatus.Authenticated
            }
        }
        if (authenticated == null || supabaseClient.auth.currentSessionOrNull() == null) {
            onProfileLoadError(OnboardingProfileError.NotAuthenticated)
            return
        }
    }

    logAccount(source)
    routeAfterAuth(profileDataSource, navigator, onProfileLoadError)
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

private fun logAccount(source: String) {
    kotlin.io.println("FosterAuth[$source]: authenticated session available")
}
