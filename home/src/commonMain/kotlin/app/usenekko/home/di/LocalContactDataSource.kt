package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.usenekko.home.addcontact.AddContactViewModel
import app.usenekko.home.data.AccountRepository
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.data.ContactProfileRepository
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.domain.Contact
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.home.presentation.contactprofile.ContactProfileViewModel
import app.usenekko.home.presentation.settings.AccountViewModel
import app.usenekko.home.presentation.settings.GroupDetailViewModel
import app.usenekko.home.presentation.settings.GroupSettingsViewModel
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.notifications.ReminderScheduler
import app.usenekko.shared.paywall.LocalPaywallGateManager
import app.usenekko.shared.paywall.PaywallGateManager
import app.usenekko.shared.subscription.LocalSubscriptionRepository
import app.usenekko.shared.subscription.SubscriptionRepository

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
    val paywallGateManager = LocalPaywallGateManager.current
    return viewModel(
        key = "home",
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    contactDataSource,
                    reminderScheduler,
                    homeRepository,
                    accountRepository,
                    paywallGateManager,
                )
            }
        },
    )
}

@Composable
fun rememberAddContactViewModel(): AddContactViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    val reminderScheduler = remember { ReminderScheduler() }
    val subscriptionRepository = LocalSubscriptionRepository.current
    val paywallGateManager = LocalPaywallGateManager.current
    return viewModel(
        key = "add-contact",
        factory = addContactViewModelFactory(
            contactDataSource = contactDataSource,
            reminderScheduler = reminderScheduler,
            subscriptionRepository = subscriptionRepository,
            homeRepository = homeRepository,
            paywallGateManager = paywallGateManager,
        ),
    )
}

fun addContactViewModelFactory(
    contactDataSource: ContactDataSource,
    reminderScheduler: ReminderScheduler,
    subscriptionRepository: SubscriptionRepository,
    homeRepository: HomeRepository? = null,
    paywallGateManager: PaywallGateManager? = null,
    editingContact: Contact? = null,
) = viewModelFactory {
    initializer {
        AddContactViewModel(
            contactDataSource = contactDataSource,
            reminderScheduler = reminderScheduler,
            subscriptionRepository = subscriptionRepository,
            homeRepository = homeRepository,
            paywallGateManager = paywallGateManager,
            editingContact = editingContact,
        )
    }
}

@Composable
fun rememberEditContactViewModel(contact: Contact): AddContactViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    val reminderScheduler = remember { ReminderScheduler() }
    val subscriptionRepository = LocalSubscriptionRepository.current
    return viewModel(
        key = "edit-contact-${contact.id}",
        factory = addContactViewModelFactory(
            contactDataSource = contactDataSource,
            reminderScheduler = reminderScheduler,
            subscriptionRepository = subscriptionRepository,
            homeRepository = homeRepository,
            editingContact = contact,
        ),
    )
}

@Composable
fun rememberContactProfileViewModel(contactId: String): ContactProfileViewModel {
    val contactDataSource = LocalContactDataSource.current
    val profileDataSource = LocalProfileDataSource.current
    val profileRepository = LocalContactProfileRepository.current
    val reminderScheduler = remember { ReminderScheduler() }
    return viewModel(
        key = "contact-profile-$contactId",
        factory = viewModelFactory {
            initializer {
                ContactProfileViewModel(
                    contactId,
                    contactDataSource,
                    reminderScheduler,
                    profileDataSource,
                    profileRepository,
                )
            }
        },
    )
}

@Composable
fun rememberAccountViewModel(): AccountViewModel {
    val homeRepository = LocalHomeRepository.current
    val accountRepository = LocalAccountRepository.current
    return viewModel(
        key = "account",
        factory = viewModelFactory {
            initializer { AccountViewModel(homeRepository, accountRepository) }
        },
    )
}

@Composable
fun rememberGroupSettingsViewModel(): GroupSettingsViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    return viewModel(
        key = "group-settings",
        factory = viewModelFactory {
            initializer { GroupSettingsViewModel(contactDataSource, homeRepository) }
        },
    )
}

@Composable
fun rememberGroupDetailViewModel(groupId: String): GroupDetailViewModel {
    val contactDataSource = LocalContactDataSource.current
    val homeRepository = LocalHomeRepository.current
    return viewModel(
        key = "group-detail-$groupId",
        factory = viewModelFactory {
            initializer { GroupDetailViewModel(groupId, contactDataSource, homeRepository) }
        },
    )
}
