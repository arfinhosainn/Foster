package app.usenekko.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.usenekko.onboarding.timereminder.TimeReminderScreen
import app.usenekko.onboarding.welcome.WelcomeScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.usenekko.home.HomeScreen
import app.usenekko.home.presentation.badges.BadgeRevealStore
import app.usenekko.home.presentation.badges.PlantUnlockedBadgeOverlay
import app.usenekko.home.presentation.brainstorm.BrainstormScreen
import app.usenekko.home.presentation.contactprofile.ContactProfileScreen
import app.usenekko.home.presentation.settings.AccountScreen
import app.usenekko.home.presentation.settings.GroupDetailScreen
import app.usenekko.home.presentation.settings.GroupSettingsScreen
import app.usenekko.home.presentation.settings.SettingScreen
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.shared.domain.Result
import app.usenekko.theme.NekkoTheme
import kotlinx.coroutines.launch

@Composable
fun OnboardingApp(navigator: Navigator) {
    OnboardingDraftStoreProvider {
        val profileDataSource = LocalOnboardingProfileDataSource.current
        val supabaseClient = LocalSupabaseClient.current
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val session = supabaseClient.auth.currentSessionOrNull()
            logAccount(session?.user?.email, session?.user?.id, "app launch")
            if (session != null) {
                routeAfterAuth(profileDataSource, navigator)
            }
        }

        val pendingBadge by BadgeRevealStore.pending.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            App(navigator) { screen ->
                when (screen) {
                is Screen.Welcome -> WelcomeScreen(
                    supabaseClient = supabaseClient,
                    onGoogleSignInSuccess = {
                        scope.launch {
                            val session = supabaseClient.auth.currentSessionOrNull()
                            logAccount(session?.user?.email, session?.user?.id, "Google sign-in")
                            routeAfterAuth(profileDataSource, navigator)
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
                    onComplete = { navigator.replaceAll(Screen.Home) },
                )

                is Screen.Notification -> NotificationScreen(
                    onNavigateToMainApp = { navigator.replaceAll(Screen.Home) },
                    onBack = { navigator.goBack() },
                )

                is Screen.Home -> HomeScreen(
                    onContactClick = { contact -> navigator.navigate(Screen.ContactProfile(contact.id)) },
                    onSettingsClick = { navigator.navigate(Screen.Settings) },
                )

                is Screen.ContactProfile -> ContactProfileScreen(
                    contactId = screen.contactId,
                    onBack = { navigator.goBack() },
                    onBrainstormClick = { navigator.navigate(Screen.Brainstorm(screen.contactId)) },
                )

                is Screen.Brainstorm -> BrainstormScreen(
                    contactId = screen.contactId,
                    onBack = { navigator.goBack() },
                )

                is Screen.Settings -> SettingScreen(
                    onBack = { navigator.goBack() },
                    onAccountClick = { navigator.navigate(Screen.Account) },
                    onGroupsClick = { navigator.navigate(Screen.GroupSettings) },
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
                    onGroupClick = { group -> navigator.navigate(Screen.GroupDetail(group.id, group.name)) },
                )

                is Screen.GroupDetail -> GroupDetailScreen(
                    groupId = screen.groupId,
                    onBack = { navigator.goBack() },
                )
            }
            }

            pendingBadge?.let { badge ->
                NekkoTheme {
                    PlantUnlockedBadgeOverlay(
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


