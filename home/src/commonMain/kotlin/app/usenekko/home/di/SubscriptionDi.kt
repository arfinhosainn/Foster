package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.usenekko.home.presentation.paywall.PaywallViewModel
import app.usenekko.shared.subscription.LocalSubscriptionRepository

@Composable
fun rememberPaywallViewModel(): PaywallViewModel {
    val subscriptionRepository = LocalSubscriptionRepository.current
    return viewModel(
        key = "paywall",
        factory = viewModelFactory {
            initializer { PaywallViewModel(subscriptionRepository) }
        },
    )
}
