package app.usenekko.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.home.data.supabase.SupabaseBrainstormDataSource
import app.usenekko.home.data.supabase.SupabaseContactDataSource
import app.usenekko.home.data.supabase.SupabaseDeleteAccountDataSource
import app.usenekko.home.data.InMemoryAccountRepository
import app.usenekko.home.data.InMemoryBrainstormRepository
import app.usenekko.home.data.InMemoryHomeRepository
import app.usenekko.home.data.InMemoryContactProfileRepository
import app.usenekko.home.di.LocalBrainstormDataSource
import app.usenekko.home.di.LocalBrainstormRepository
import app.usenekko.home.di.LocalAccountRepository
import app.usenekko.home.di.LocalContactDataSource
import app.usenekko.home.di.LocalDeleteAccountDataSource
import app.usenekko.home.di.LocalHomeRepository
import app.usenekko.home.di.LocalContactProfileRepository
import app.usenekko.home.di.LocalProfileDataSource
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.subscription.LocalSubscriptionRepository
import app.usenekko.shared.subscription.RevenueCatSubscriptionRepository
import app.usenekko.onboarding.addnote.AddNoteViewModel
import app.usenekko.onboarding.contact.ContactViewModel
import app.usenekko.onboarding.customreminder.CustomReminderViewModel
import app.usenekko.onboarding.data.rememberOnboardingDraftDataSource
import app.usenekko.onboarding.data.supabase.SupabaseOnboardingProfileDataSource
import app.usenekko.onboarding.data.supabase.createAppSupabaseClient
import app.usenekko.onboarding.dayreminder.ReminderViewModel
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.group.GroupViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import app.usenekko.onboarding.name.NameViewModel
import app.usenekko.onboarding.notification.NotificationViewModel
import app.usenekko.onboarding.timereminder.TimeReminderViewModel

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
