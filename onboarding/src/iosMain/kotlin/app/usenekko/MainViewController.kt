package app.usenekko

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.contact.ContactScreen
import app.usenekko.onboarding.group.GroupScreen
import app.usenekko.onboarding.name.NameScreen
import app.usenekko.onboarding.phone.CodeVerificationScreen
import app.usenekko.onboarding.phone.PhoneScreen
import app.usenekko.onboarding.reminder.ReminderScreen
import app.usenekko.onboarding.timereminder.TimeReminderScreen
import app.usenekko.onboarding.customreminder.CustomReminderScreen
import app.usenekko.onboarding.addnote.AddNoteScreen
import app.usenekko.onboarding.notification.NotificationScreen
import app.usenekko.onboarding.welcome.WelcomeScreen

fun MainViewController() = ComposeUIViewController {
    val navigator = remember { Navigator(startDestination = Screen.Welcome) }
    App(navigator) { screen ->
        when (screen) {
            is Screen.Welcome -> WelcomeScreen(
                onNavigateToPhone = { navigator.navigate(Screen.Phone) },
            )
            is Screen.Phone -> PhoneScreen(
                onNavigateToCodeVerification = { phoneNumber ->
                    navigator.navigate(Screen.CodeVerification(phoneNumber))
                },
                onBack = { navigator.goBack() },
                onSkip = { navigator.navigate(Screen.Name) },
            )
            is Screen.CodeVerification -> CodeVerificationScreen(
                phoneNumber = screen.phoneNumber,
                onNavigateToNext = { navigator.navigate(Screen.Name) },
                onBack = { navigator.goBack() },
                onSkip = { navigator.navigate(Screen.Name) },
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
                onNavigateToNext = { /* TODO */ },
                onBack = { navigator.goBack() },
                onSkip = { /* TODO */ },
            )
        }
    }
}
