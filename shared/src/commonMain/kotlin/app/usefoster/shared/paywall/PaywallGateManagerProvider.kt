package app.usefoster.shared.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import app.usefoster.shared.subscription.LocalSubscriptionRepository

/** App-wide access to the discount-paywall decision engine. */
val LocalPaywallGateManager = staticCompositionLocalOf<PaywallGateManager> {
    error("PaywallGateManager not provided")
}

/**
 * Provides [PaywallGateManager] for the whole app tree. Must sit below
 * OnboardingDraftStoreProvider (needs LocalSubscriptionRepository). Calls
 * [PaywallGateManager.onAppStart] exactly once per cold start.
 */
@Composable
fun PaywallGateManagerProvider(content: @Composable () -> Unit) {
    val subscriptionRepository = LocalSubscriptionRepository.current
    val manager = rememberPaywallGateManager(subscriptionRepository)

    LaunchedEffect(manager) {
        manager.onAppStart()
    }

    CompositionLocalProvider(LocalPaywallGateManager provides manager) {
        content()
    }
}

@Composable
expect fun rememberPaywallGateDataSource(): PaywallGateDataSource