package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.usenekko.home.presentation.paywall.DiscountPaywallViewModel
import app.usenekko.home.presentation.paywall.PaywallViewModel
import app.usenekko.shared.paywall.LocalPaywallGateManager
import app.usenekko.shared.subscription.LocalSubscriptionRepository

@Composable
fun rememberPaywallViewModel(): PaywallViewModel {
    val subscriptionRepository = LocalSubscriptionRepository.current
    val paywallGateManager = LocalPaywallGateManager.current
    return viewModel(
        key = "paywall",
        factory = viewModelFactory {
            initializer { PaywallViewModel(subscriptionRepository, paywallGateManager) }
        },
    )
}

@Composable
fun rememberDiscountPaywallViewModel(): DiscountPaywallViewModel {
    val subscriptionRepository = LocalSubscriptionRepository.current
    val paywallGateManager = LocalPaywallGateManager.current
    return viewModel(
        key = "discount_paywall",
        factory = viewModelFactory {
            initializer { DiscountPaywallViewModel(subscriptionRepository, paywallGateManager) }
        },
    )
}
