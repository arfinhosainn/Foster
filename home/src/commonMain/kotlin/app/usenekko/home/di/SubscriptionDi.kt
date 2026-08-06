package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.usenekko.home.presentation.paywall.PaywallViewModel
import app.usenekko.shared.subscription.LocalSubscriptionRepository

@Composable
fun rememberPaywallViewModel(): PaywallViewModel {
    val subscriptionRepository = LocalSubscriptionRepository.current
    return remember { PaywallViewModel(subscriptionRepository) }
}
