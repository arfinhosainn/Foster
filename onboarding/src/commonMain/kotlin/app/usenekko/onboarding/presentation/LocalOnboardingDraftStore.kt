package app.usenekko.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.onboarding.addnote.AddNoteViewModel
import app.usenekko.onboarding.contact.ContactViewModel
import app.usenekko.onboarding.customreminder.CustomReminderViewModel
import app.usenekko.onboarding.data.rememberOnboardingDraftDataSource
import app.usenekko.onboarding.data.supabase.SupabaseOnboardingProfileDataSource
import app.usenekko.onboarding.data.supabase.createAppSupabaseClient
import app.usenekko.onboarding.dayreminder.ReminderViewModel
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.group.GroupViewModel
import app.usenekko.onboarding.name.NameViewModel
import app.usenekko.onboarding.email.EmailVerificationViewModel
import app.usenekko.onboarding.email.EmailViewModel
import app.usenekko.onboarding.notification.NotificationViewModel
import app.usenekko.onboarding.timereminder.TimeReminderViewModel

val LocalOnboardingDraftStore = staticCompositionLocalOf<OnboardingDraftStore> {
    error("No OnboardingDraftStore provided.")
}

val LocalOnboardingProfileDataSource = staticCompositionLocalOf<OnboardingProfileDataSource> {
    error("No OnboardingProfileDataSource provided.")
}

@Composable
fun OnboardingDraftStoreProvider(
    content: @Composable () -> Unit,
) {
    val dataSource = rememberOnboardingDraftDataSource()
    val draftStore = remember { OnboardingDraftStore(dataSource) }

    val supabaseClient = remember { createAppSupabaseClient() }
    val profileDataSource = remember { SupabaseOnboardingProfileDataSource(supabaseClient) }

    CompositionLocalProvider(
        LocalOnboardingDraftStore provides draftStore,
        LocalOnboardingProfileDataSource provides profileDataSource,
    ) {
        content()
    }
}

@Composable
fun rememberEmailViewModel(): EmailViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { EmailViewModel(draftStore) }
}

@Composable
fun rememberEmailVerificationViewModel(email: String): EmailVerificationViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember(email) { EmailVerificationViewModel(email, draftStore) }
}

@Composable
fun rememberNameViewModel(): NameViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { NameViewModel(draftStore) }
}

@Composable
fun rememberContactViewModel(): ContactViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { ContactViewModel(draftStore) }
}

@Composable
fun rememberGroupViewModel(): GroupViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { GroupViewModel(draftStore) }
}

@Composable
fun rememberReminderViewModel(): ReminderViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { ReminderViewModel(draftStore) }
}

@Composable
fun rememberTimeReminderViewModel(): TimeReminderViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { TimeReminderViewModel(draftStore) }
}

@Composable
fun rememberCustomReminderViewModel(): CustomReminderViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { CustomReminderViewModel(draftStore) }
}

@Composable
fun rememberAddNoteViewModel(): AddNoteViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    return remember { AddNoteViewModel(draftStore) }
}

@Composable
fun rememberNotificationViewModel(): NotificationViewModel {
    val draftStore = LocalOnboardingDraftStore.current
    val profileDataSource = LocalOnboardingProfileDataSource.current
    return remember { NotificationViewModel(draftStore, profileDataSource) }
}
