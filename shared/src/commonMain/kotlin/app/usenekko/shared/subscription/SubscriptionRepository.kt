package app.usenekko.shared.subscription

import app.usenekko.shared.domain.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Entitlement identifier configured in the RevenueCat dashboard. A single
 * entitlement unlocks every gated feature (contacts + brainstorming), so
 * subscribing once lifts BOTH gates.
 */
const val UNLIMITED_ENTITLEMENT_ID = "unlimited"

/**
 * App-wide subscription state, backed by RevenueCat's `unlimited` entitlement.
 *
 * Read [isSubscribed] from anywhere (Home / AddContact / Brainstorm /
 * Settings). It is a hot [StateFlow] that updates reactively right after a
 * purchase or restore completes — no app restart needed — so whatever gate
 * triggered the paywall unblocks immediately on success.
 */
interface SubscriptionRepository {

    /** True while the `unlimited` entitlement is active for the current user. */
    val isSubscribed: StateFlow<Boolean>

    /** Re-fetch entitlement from RevenueCat (call on app foreground / after auth). */
    suspend fun refresh(): Result<Unit, SubscriptionError>

    /** Current Offering's monthly + annual packages, with store prices + trial info. */
    suspend fun loadPaywallOffering(): Result<PaywallOffering, SubscriptionError>

    /** Trigger RevenueCat's purchase flow for [pkg]. */
    suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome

    /** Restore previous purchases (reinstall / device switch). App Store-required. */
    suspend fun restorePurchases(): Result<Boolean, SubscriptionError>
}

enum class BillingPeriod { MONTHLY, ANNUAL }

/**
 * A single purchasable plan on the paywall. All price/period text comes from
 * the store via RevenueCat (never hardcoded) so it is always correct per region
 * & currency. [hasFreeTrial] is auto-detected from the package's introductory
 * offer at runtime — the CTA adapts to "Continue with 7-day free trial" vs
 * "Subscribe" automatically.
 */
data class PaywallPackage(
    val identifier: String,
    val period: BillingPeriod,
    val priceString: String,
    val periodString: String,
    val hasFreeTrial: Boolean,
    val trialString: String?,
)

data class PaywallOffering(
    val monthly: PaywallPackage?,
    val annual: PaywallPackage?,
)

sealed interface PurchaseOutcome {
    data object Success : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data object Error : PurchaseOutcome
}

sealed interface SubscriptionError {
    data object NotConfigured : SubscriptionError
    data object Network : SubscriptionError
    data class Unknown(val detail: String?) : SubscriptionError
}
