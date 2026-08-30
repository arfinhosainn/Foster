package app.usefoster.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import app.usefoster.home.data.supabase.SupabaseBrainstormDataSource
import app.usefoster.home.data.supabase.SupabaseContactDataSource
import app.usefoster.home.data.supabase.SupabaseDeleteAccountDataSource
import app.usefoster.home.data.InMemoryAccountRepository
import app.usefoster.home.data.InMemoryBrainstormRepository
import app.usefoster.home.data.InMemoryHomeRepository
import app.usefoster.home.data.InMemoryContactProfileRepository
import app.usefoster.home.di.LocalBrainstormDataSource
import app.usefoster.home.di.LocalBrainstormRepository
import app.usefoster.home.di.LocalAccountRepository
import app.usefoster.home.di.LocalContactDataSource
import app.usefoster.home.di.LocalDeleteAccountDataSource
import app.usefoster.home.di.LocalHomeRepository
import app.usefoster.home.di.LocalContactProfileRepository
import app.usefoster.home.di.LocalProfileDataSource
import app.usefoster.shared.domain.ProfileDataSource
import app.usefoster.shared.subscription.LocalSubscriptionRepository
import app.usefoster.shared.subscription.RevenueCatSubscriptionRepository
import app.usefoster.onboarding.addnote.AddNoteViewModel
import app.usefoster.onboarding.contact.ContactViewModel
import app.usefoster.onboarding.customreminder.CustomReminderViewModel
import app.usefoster.onboarding.data.rememberOnboardingDraftDataSource
import app.usefoster.onboarding.data.supabase.SupabaseOnboardingProfileDataSource
import app.usefoster.onboarding.data.supabase.createAppSupabaseClient
import app.usefoster.onboarding.dayreminder.ReminderViewModel
import app.usefoster.onboarding.domain.OnboardingProfileDataSource
import app.usefoster.onboarding.group.GroupViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import app.usefoster.onboarding.name.NameViewModel
import app.usefoster.onboarding.notification.NotificationViewModel
import app.usefoster.onboarding.timereminder.TimeReminderViewModel

val LocalOnboardingDraftStore = staticCompositionLocalOf<OnboardingDraftStore> {
    error("No OnboardingDraftStore provided.")
}

val LocalOnboardingProfileDataSource = staticCompositionLocalOf<OnboardingProfileDataSource> {
    error("No OnboardingProfileDataSource provided.")
}

val LocalSupabaseClient = staticCompositionLocalOf<SupabaseClient> {
    error("No SupabaseClient provided.")
}

@Composable
fun OnboardingDraftStoreProvider(
    supabaseClient: SupabaseClient? = null,
    content: @Composable () -> Unit,
) {
    val dataSource = rememberOnboardingDraftDataSource()
    val draftStore = remember { OnboardingDraftStore(dataSource) }

    val client = remember(supabaseClient) { supabaseClient ?: createAppSupabaseClient() }
    val profileDataSource = remember(client) { SupabaseOnboardingProfileDataSource(client) }
    val contactDataSource = remember(client) { SupabaseContactDataSource(client) }
    val repositoryScope = rememberCoroutineScope()
    val homeRepository = remember(client) {
        InMemoryHomeRepository(
            contactDataSource = contactDataSource,
            accountKeyProvider = { client.auth.currentSessionOrNull()?.user?.id },
            scope = repositoryScope,
        )
    }
    val accountRepository = remember(client) {
        InMemoryAccountRepository(
            profileDataSource = profileDataSource,
            contactDataSource = contactDataSource,
            accountKeyProvider = { client.auth.currentSessionOrNull()?.user?.id },
            scope = repositoryScope,
        )
    }
    val contactProfileRepository = remember(client) {
        InMemoryContactProfileRepository(
            contactDataSource = contactDataSource,
            accountKeyProvider = { client.auth.currentSessionOrNull()?.user?.id },
            scope = repositoryScope,
        )
    }
    val deleteAccountDataSource = remember(client) { SupabaseDeleteAccountDataSource(client) }
    val brainstormDataSource = remember(client) { SupabaseBrainstormDataSource(client) }
    val brainstormRepository = remember(client) {
        InMemoryBrainstormRepository(
            dataSource = brainstormDataSource,
            accountKeyProvider = { client.auth.currentSessionOrNull()?.user?.id },
            scope = repositoryScope,
        )
    }
    val subscriptionRepository = remember { RevenueCatSubscriptionRepository() }

    CompositionLocalProvider(
        LocalOnboardingDraftStore provides draftStore,
        LocalOnboardingProfileDataSource provides profileDataSource,
        LocalProfileDataSource provides (profileDataSource as ProfileDataSource),
        LocalSupabaseClient provides client,
        LocalContactDataSource provides contactDataSource,
        LocalHomeRepository provides homeRepository,
        LocalAccountRepository provides accountRepository,
        LocalContactProfileRepository provides contactProfileRepository,
        LocalDeleteAccountDataSource provides deleteAccountDataSource,
        LocalBrainstormDataSource provides brainstormDataSource,
        LocalBrainstormRepository provides brainstormRepository,
        LocalSubscriptionRepository provides subscriptionRepository,
    ) {
        content()
    }
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
