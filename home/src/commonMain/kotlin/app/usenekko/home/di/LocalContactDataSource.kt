package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.home.addcontact.AddContactViewModel
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.home.presentation.HomeViewModel
import app.usenekko.home.presentation.contactprofile.ContactProfileViewModel

val LocalContactDataSource = staticCompositionLocalOf<ContactDataSource> {
    error("ContactDataSource not provided")
}

@Composable
fun rememberHomeViewModel(): HomeViewModel {
    val contactDataSource = LocalContactDataSource.current
    return remember { HomeViewModel(contactDataSource) }
}

@Composable
fun rememberAddContactViewModel(): AddContactViewModel {
    val contactDataSource = LocalContactDataSource.current
    return remember { AddContactViewModel(contactDataSource) }
}

@Composable
fun rememberContactProfileViewModel(contactId: String): ContactProfileViewModel {
    val contactDataSource = LocalContactDataSource.current
    return remember(contactId) { ContactProfileViewModel(contactId, contactDataSource) }
}
