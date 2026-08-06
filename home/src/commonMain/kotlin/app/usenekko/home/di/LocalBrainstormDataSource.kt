package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.home.domain.BrainstormDataSource
import app.usenekko.home.presentation.brainstorm.BrainstormViewModel
import app.usenekko.shared.subscription.LocalSubscriptionRepository

val LocalBrainstormDataSource = staticCompositionLocalOf<BrainstormDataSource> {
    error("BrainstormDataSource not provided")
}

@Composable
fun rememberBrainstormViewModel(contactId: String): BrainstormViewModel {
    val dataSource = LocalBrainstormDataSource.current
    val subscriptionRepository = LocalSubscriptionRepository.current
    return remember(contactId) { BrainstormViewModel(contactId, dataSource, subscriptionRepository) }
}
