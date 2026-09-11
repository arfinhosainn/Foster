package app.usefoster.shared.subscription

import app.usefoster.shared.domain.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Entitlement identifier configured in the RevenueCat dashboard. A single
 * entitlement unlocks every gated feature (contacts + brainstorming), so
 * subscribing once lifts BOTH gates.
 */
const val UNLIMITED_ENTITLEMENT_ID = "unlimited"

/** Optional RevenueCat offering used by the time-limited discount paywall. */
const val DISCOUNT_OFFERING_ID = "discount"

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

    /** True when the active subscription was canceled and will end at expiry. */
    val isCancellationPending: StateFlow<Boolean>

    /**
     * Store product id of the active subscription that grants the `unlimited`
     * entitlement (e.g. `foster_premium_yearly`). Null when not subscribed.
     * Updated on every refresh / customer-info event so it stays correct when
     * the user cancels and the entitlement lapses.
     */
    val activeProductId: StateFlow<String?>

    /**
     * True when the store reported a billing problem with the subscription that
     * unlocks `unlimited` (payment method failed / renewal declined). The
     * entitlement usually stays ACTIVE through the store's grace period, so
     * [isSubscribed] may still be true — but when it is false and this is true,
     * the correct move is to deep-link the user to fix their payment method on
     * the store, not to show them a buy screen.
     */
    val hasBillingIssue: StateFlow<Boolean>

    /**
     * Native store URL where the user can view, update payment method or cancel
     * their active subscription. On Android this is Google Play's subscriptions
     * page (`https://play.google.com/store/account/subscriptions?sku=<product-id>
     * &package=app.usefoster`) and on iOS the App Store's subscription settings
     * page. RevenueCat derives the correct URL per platform from the latest
     * [CustomerInfo]; the product-id deep link (then the bare package link) is
     * used as a fallback.
     *
     * Returns null when the user has no active subscription / payment issue.
     * One tap from Settings lands the user on Play's native management screen,
     * satisfying Play's "two taps to cancel" policy without any in-app UI.
     */
    fun manageSubscriptionUrl(): String?

    /** Re-fetch entitlement from RevenueCat (call on app foreground / after auth). */
    suspend fun refresh(): Result<Unit, SubscriptionError>

    /**
     * Align RevenueCat with the authenticated account. Pass null when the app
     * signs out so the next account starts from RevenueCat's anonymous user.
     */
    suspend fun identify(userId: String?): Result<Unit, SubscriptionError>

    /**
     * Load an offering's monthly + annual packages, with store prices + trial
     * info. When [offeringIdentifier] is absent or not configured, the current
     * offering is used.
     */
    suspend fun loadPaywallOffering(
        offeringIdentifier: String? = null,
    ): Result<PaywallOffering, SubscriptionError>

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
