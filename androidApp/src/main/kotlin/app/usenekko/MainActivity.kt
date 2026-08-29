package app.usenekko

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
import app.usenekko.navigation.Screen
import app.usenekko.navigation.rememberNavigator
import app.usenekko.onboarding.OnboardingApp
import app.usenekko.onboarding.data.supabase.createAppSupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import app.usenekko.shared.notifications.NotificationTapExtras
import app.usenekko.shared.notifications.NotificationTapRouter
import app.usenekko.shared.notifications.NotificationTarget
import app.usenekko.shared.notifications.ReminderScheduler
import app.usenekko.shared.subscription.initRevenueCat

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
            // a real screen is up.
            val pendingTap by NotificationTapRouter.pending.collectAsState()
            LaunchedEffect(pendingTap, navigator.currentScreen) {
                val target = pendingTap ?: return@LaunchedEffect
                if (navigator.currentScreen is Screen.Splash) return@LaunchedEffect
                NotificationTapRouter.consume()
                val dayKey = target.dayKey
                val contactId = target.contactId
                when {
                    dayKey != null -> navigator.navigate(Screen.DayAgenda(dayKey))
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
