package app.usefoster.home.presentation.paywall

import app.usefoster.shared.subscription.PaywallPackage
import org.jetbrains.compose.resources.StringResource

sealed interface DiscountPaywallAction {
    data object Purchase : DiscountPaywallAction
    data object Restore : DiscountPaywallAction
}

sealed interface DiscountPaywallEvent {
    /** Emitted after a successful purchase OR a restore that found an active subscription. */
    data object Subscribed : DiscountPaywallEvent
    data class ShowError(val message: StringResource) : DiscountPaywallEvent
}

data class DiscountPaywallState(
    val isLoading: Boolean = true,
    val annual: PaywallPackage? = null,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
)
