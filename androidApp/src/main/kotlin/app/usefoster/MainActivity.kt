package app.usefoster

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.usefoster.navigation.Screen
import app.usefoster.navigation.rememberNavigator
import app.usefoster.onboarding.OnboardingApp
import app.usefoster.onboarding.data.supabase.createAppSupabaseClient
import app.usefoster.onboarding.isFirstRunSurface
import io.github.jan.supabase.auth.handleDeeplinks
import app.usefoster.shared.notifications.NotificationTapExtras
import app.usefoster.shared.notifications.NotificationTapRouter
import app.usefoster.shared.notifications.NotificationTarget
import app.usefoster.shared.notifications.HomeCheckInListSignal
import app.usefoster.shared.notifications.ReminderScheduler
import app.usefoster.shared.subscription.initRevenueCat

class MainActivity : ComponentActivity() {
    private val supabaseClient by lazy { createAppSupabaseClient() }

    // Holds the Android 12+ system splash on screen until the initial
    // auth/session routing settles (Splash -> real screen) or the profile
    // check fails (the splash error/retry UI must become visible). Kept in
    // sync by OnboardingApp via onSplashBusyChanged.
    private val splashHeldVisible = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { splashHeldVisible.value }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        supabaseClient.handleDeeplinks(intent)

        ReminderScheduler.init(applicationContext)
        initRevenueCat()
        routeNotificationTap(intent)

        setContent {
            val navigator = rememberNavigator(startDestination = Screen.Splash)
            BackHandler(enabled = navigator.canGoBack) {
                navigator.goBack()
            }
            OnboardingApp(navigator, supabaseClient) { busy ->
                splashHeldVisible.value = busy
            }

            // Notification tap routing — buffered so cold-start taps (extras
            // arriving before the nav graph/auth routing is ready) replay once
            // a real screen is up. Onboarding surfaces are never hijacked; the
            // tap stays buffered and replays after onboarding completes.
            val pendingTap by NotificationTapRouter.pending.collectAsState()
            LaunchedEffect(pendingTap, navigator.currentScreen) {
                val target = pendingTap ?: return@LaunchedEffect
                if (navigator.currentScreen is Screen.Splash) return@LaunchedEffect
                if (navigator.currentScreen.isFirstRunSurface) return@LaunchedEffect
                NotificationTapRouter.consume()
                val dayKey = target.dayKey
                val contactId = target.contactId
                when {
                    dayKey != null -> {
                        // Group digest → land on Home scrolled to the check-in
                        // contact list; no separate day-agenda screen.
                        if (navigator.currentScreen !is Screen.Home) {
                            navigator.replaceAll(Screen.Home)
                        }
                        HomeCheckInListSignal.post()
                    }
                    contactId != null -> navigator.navigate(Screen.ContactProfile(contactId))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        supabaseClient.handleDeeplinks(intent)
        routeNotificationTap(intent)
    }

    private fun routeNotificationTap(intent: Intent?) {
        val extras = intent?.extras ?: return
        val dayKey = if (extras.containsKey(NotificationTapExtras.EXTRA_OPEN_DAY_KEY)) {
            extras.getLong(NotificationTapExtras.EXTRA_OPEN_DAY_KEY)
        } else {
            null
        }
        val contactId = extras.getString(NotificationTapExtras.EXTRA_OPEN_CONTACT_ID)
        if (dayKey != null || contactId != null) {
            NotificationTapRouter.post(NotificationTarget(dayKey = dayKey, contactId = contactId))
        }
    }
}
