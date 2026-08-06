package app.usenekko.home.presentation.paywall

import app.usenekko.shared.subscription.BillingPeriod
import app.usenekko.shared.subscription.PaywallOffering
import app.usenekko.shared.subscription.PaywallPackage

sealed interface PaywallAction {
    data class SelectPeriod(val period: BillingPeriod) : PaywallAction
    data object Purchase : PaywallAction
    data object Restore : PaywallAction
    data object DismissError : PaywallAction
}

sealed interface PaywallEvent {
    /** Emitted after a successful purchase OR a restore that found an active subscription. */
    data object Subscribed : PaywallEvent
}

data class PaywallState(
    val isLoading: Boolean = true,
    val offering: PaywallOffering? = null,
    val selectedPeriod: BillingPeriod = BillingPeriod.ANNUAL,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val error: String? = null,
) {
    val selectedPackage: PaywallPackage?
        get() = when (selectedPeriod) {
            BillingPeriod.MONTHLY -> offering?.monthly
            BillingPeriod.ANNUAL -> offering?.annual
        }

    /**
     * CTA adapts automatically based on the selected package's intro offer:
     * "Continue with 7-day free trial" if a free trial is detected from the
     * store, "Subscribe" otherwise. Never hardcoded.
     */
    val ctaText: String
        get() {
            val pkg = selectedPackage ?: return "Subscribe"
            return if (pkg.hasFreeTrial) {
                "Continue with ${pkg.trialString ?: "free trial"}"
            } else {
                "Subscribe"
            }
        }
}
