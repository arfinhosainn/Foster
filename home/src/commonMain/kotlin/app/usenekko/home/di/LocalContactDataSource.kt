package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.home.addcontact.AddContactViewModel
import app.usenekko.home.data.AccountRepository
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.data.ContactProfileRepository
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.home.presentation.contactprofile.ContactProfileViewModel
import app.usenekko.home.presentation.settings.AccountViewModel
import app.usenekko.home.presentation.settings.GroupDetailViewModel
import app.usenekko.home.presentation.settings.GroupSettingsViewModel
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.notifications.ReminderScheduler
import app.usenekko.shared.subscription.LocalSubscriptionRepository

val LocalContactDataSource = staticCompositionLocalOf<ContactDataSource> {
    error("ContactDataSource not provided")
}

val LocalHomeRepository = staticCompositionLocalOf<HomeRepository> {
    error("HomeRepository not provided")
}

val LocalAccountRepository = staticCompositionLocalOf<AccountRepository> {
    error("AccountRepository not provided")
}

val LocalContactProfileRepository = staticCompositionLocalOf<ContactProfileRepository> {
    error("Home contact profile repository not provided")
}

val LocalProfileDataSource = staticCompositionLocalOf<ProfileDataSource> {
    error("ProfileDataSource not provided")
}

@Composable
fun rememberHomeViewModel(): HomeViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    val accountRepository = LocalAccountRepository.current
    val reminderScheduler = remember { ReminderScheduler() }
    return remember {
        HomeViewModel(contactDataSource, reminderScheduler, homeRepository, accountRepository)
    }
}

@Composable
fun rememberAddContactViewModel(): AddContactViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    val reminderScheduler = remember { ReminderScheduler() }
    val subscriptionRepository = LocalSubscriptionRepository.current
    return remember {
        AddContactViewModel(contactDataSource, reminderScheduler, subscriptionRepository, homeRepository)
    }
}

@Composable
fun rememberContactProfileViewModel(contactId: String): ContactProfileViewModel {
    val contactDataSource = LocalContactDataSource.current
    val profileDataSource = LocalProfileDataSource.current
    val profileRepository = LocalContactProfileRepository.current
    val reminderScheduler = remember { ReminderScheduler() }
    return remember(contactId) {
        ContactProfileViewModel(
            contactId,
            contactDataSource,
            reminderScheduler,
            profileDataSource,
            profileRepository,
        )
    }
}

@Composable
fun rememberAccountViewModel(): AccountViewModel {
    val homeRepository = LocalHomeRepository.current
    val accountRepository = LocalAccountRepository.current
    return remember { AccountViewModel(homeRepository, accountRepository) }
}

@Composable
fun rememberGroupSettingsViewModel(): GroupSettingsViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    return remember { GroupSettingsViewModel(contactDataSource, homeRepository) }
}

@Composable
fun rememberGroupDetailViewModel(groupId: String): GroupDetailViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    return remember(groupId) { GroupDetailViewModel(groupId, contactDataSource, homeRepository) }
}
