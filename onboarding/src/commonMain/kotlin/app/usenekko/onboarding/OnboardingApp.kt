package app.usenekko.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.jan.supabase.auth.auth
import app.usenekko.onboarding.timereminder.TimeReminderScreen
import app.usenekko.onboarding.welcome.WelcomeScreen
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.domain.Result

@Composable
fun OnboardingApp(navigator: Navigator) {
    OnboardingDraftStoreProvider {
        val profileDataSource = LocalOnboardingProfileDataSource.current
        val supabaseClient = LocalSupabaseClient.current

        LaunchedEffect(Unit) {
            if (supabaseClient.auth.currentSessionOrNull() != null) {
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
        }

        App(navigator) { screen ->
            when (screen) {
                is Screen.Welcome -> WelcomeScreen(
                    supabaseClient = supabaseClient,
                    onGoogleSignInSuccess = {
                        navigator.replaceAll(Screen.Name)
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
                    onNavigateToMainApp = { navigator.replaceAll(Screen.Home) },
                    onBack = { navigator.goBack() },
                )

                is Screen.Home -> HomePlaceholder()
            }
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

@Composable
private fun HomePlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Home")
    }
}
