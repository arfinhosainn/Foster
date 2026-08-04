package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.home.addcontact.AddContactViewModel
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.home.presentation.contactprofile.ContactProfileViewModel
import app.usenekko.home.presentation.settings.AccountViewModel
import app.usenekko.home.presentation.settings.GroupDetailViewModel
import app.usenekko.home.presentation.settings.GroupSettingsViewModel
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.notifications.ReminderScheduler

val LocalContactDataSource = staticCompositionLocalOf<ContactDataSource> {
    error("ContactDataSource not provided")
}

val LocalProfileDataSource = staticCompositionLocalOf<ProfileDataSource> {
    error("ProfileDataSource not provided")
}

@Composable
fun rememberHomeViewModel(): HomeViewModel {
    val contactDataSource = LocalContactDataSource.current
    val reminderScheduler = remember { ReminderScheduler() }
    return remember {
        HomeViewModel(contactDataSource, reminderScheduler)
    }
}

@Composable
fun rememberAddContactViewModel(): AddContactViewModel {
    val contactDataSource = LocalContactDataSource.current
    val reminderScheduler = remember { ReminderScheduler() }
    return remember { AddContactViewModel(contactDataSource, reminderScheduler) }
}

@Composable
fun rememberContactProfileViewModel(contactId: String): ContactProfileViewModel {
    val contactDataSource = LocalContactDataSource.current
    val reminderScheduler = remember { ReminderScheduler() }
    return remember(contactId) { ContactProfileViewModel(contactId, contactDataSource, reminderScheduler) }
}

@Composable
fun rememberAccountViewModel(): AccountViewModel {
    val profileDataSource = LocalProfileDataSource.current
    val contactDataSource = LocalContactDataSource.current
    return remember { AccountViewModel(profileDataSource, contactDataSource) }
}

@Composable
fun rememberGroupSettingsViewModel(): GroupSettingsViewModel {
    val contactDataSource = LocalContactDataSource.current
    return remember { GroupSettingsViewModel(contactDataSource) }
}

@Composable
fun rememberGroupDetailViewModel(groupId: String): GroupDetailViewModel {
    val contactDataSource = LocalContactDataSource.current
    return remember(groupId) { GroupDetailViewModel(groupId, contactDataSource) }
}
