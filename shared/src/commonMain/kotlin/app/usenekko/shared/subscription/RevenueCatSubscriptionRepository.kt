package app.usenekko.shared.subscription

import app.usenekko.shared.domain.Result
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.DiscountPaymentMode
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.freePhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RevenueCat-backed [SubscriptionRepository]. A single `unlimited` entitlement
 * unlocks every gated feature (contacts + brainstorming).
 *
 * Entitlement state ([isSubscribed]) is a hot [StateFlow] that updates
 * immediately after a purchase or restore — no app restart needed — so whatever
 * gate triggered the paywall unblocks the instant the purchase succeeds.
 */
class RevenueCatSubscriptionRepository : SubscriptionRepository {

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    // RC Package objects keyed by identifier, cached when the offering is loaded
    // so purchase() can resolve the UI-facing PaywallPackage back to the real
    // RC Package that awaitPurchase() needs.
    private var cachedPackages: Map<String, Package> = emptyMap()

    override suspend fun refresh() {
        if (!Purchases.isConfigured) return
        try {
            val info = Purchases.sharedInstance.awaitCustomerInfo()
            _isSubscribed.value = isUnlimitedActive(info)
        } catch (_: Exception) {
            // Keep last-known state on transient errors.
        }
    }

    override suspend fun loadPaywallOffering(): Result<PaywallOffering, SubscriptionError> {
        if (!Purchases.isConfigured) return Result.Error(SubscriptionError.NotConfigured)
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val current = offerings.current
                ?: return Result.Error(SubscriptionError.NotConfigured)
            Result.Success(current.toPaywallOffering())
        } catch (e: PurchasesException) {
            Result.Error(e.toSubscriptionError())
        } catch (e: Exception) {
            Result.Error(SubscriptionError.Unknown(e.message))
        }
    }

    override suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome {
        if (!Purchases.isConfigured) return PurchaseOutcome.Error("RevenueCat not configured")
        val rcPackage = cachedPackages[pkg.identifier]
            ?: return PurchaseOutcome.Error("Selected plan is no longer available")
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(rcPackage)
            // Flip entitlement immediately — no restart needed.
            _isSubscribed.value = isUnlimitedActive(result.customerInfo)
            PurchaseOutcome.Success
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) PurchaseOutcome.Cancelled
            else PurchaseOutcome.Error(e.message)
        } catch (e: Exception) {
            PurchaseOutcome.Error(e.message)
        }
    }

    override suspend fun restorePurchases(): Result<Boolean, SubscriptionError> {
        if (!Purchases.isConfigured) return Result.Error(SubscriptionError.NotConfigured)
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            val active = isUnlimitedActive(info)
            _isSubscribed.value = active
            Result.Success(active)
        } catch (e: PurchasesException) {
            Result.Error(e.toSubscriptionError())
        } catch (e: Exception) {
            Result.Error(SubscriptionError.Unknown(e.message))
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private fun isUnlimitedActive(info: CustomerInfo): Boolean =
        info.entitlements.active[UNLIMITED_ENTITLEMENT_ID] != null

    private fun Offering.toPaywallOffering(): PaywallOffering {
        val packages = mutableMapOf<String, Package>()
        monthly?.let { packages[it.identifier] = it }
        annual?.let { packages[it.identifier] = it }
        cachedPackages = packages
        return PaywallOffering(
            monthly = monthly?.toPaywallPackage(BillingPeriod.MONTHLY),
            annual = annual?.toPaywallPackage(BillingPeriod.ANNUAL),
        )
    }

    private fun Package.toPaywallPackage(period: BillingPeriod): PaywallPackage {
        val product = storeProduct
        val (hasTrial, trialString) = product.freeTrialInfo()
        return PaywallPackage(
            identifier = identifier,
            period = period,
            priceString = product.price.formatted,
            periodString = when (period) {
                BillingPeriod.MONTHLY -> "per month"
                BillingPeriod.ANNUAL -> "per year"
            },
            hasFreeTrial = hasTrial,
            trialString = trialString,
        )
    }

    /**
     * Auto-detects a free-trial introductory offer from the store so the CTA can
     * read "Continue with 7-day free trial" when one exists, or "Subscribe" when
     * it doesn't — never hardcoded, adapts to whatever is configured in the stores.
     *
     * - iOS: [StoreProduct.introductoryDiscount] with [DiscountPaymentMode.FREE_TRIAL].
     * - Android: [StoreProduct.subscriptionOptions] → freeTrial option → freePhase.
     */
    @Suppress("unused")
    private fun StoreProduct.freeTrialInfo(): Pair<Boolean, String?> {
        // iOS: introductory discount with a free-trial payment mode.
        introductoryDiscount?.let { discount ->
            if (discount.paymentMode == DiscountPaymentMode.FREE_TRIAL) {
                return true to discount.subscriptionPeriod.formatTrial()
            }
        }
        // Android: subscription options → free trial phase.
        val freeTrialOption = subscriptionOptions?.freeTrial
        if (freeTrialOption != null) {
            val phase = freeTrialOption.freePhase
            return if (phase != null) {
                true to phase.billingPeriod.formatTrial()
            } else {
                true to null
            }
        }
        return false to null
    }

    private fun Period.formatTrial(): String {
        val unitName = unit.name.lowercase()
        val label = if (value == 1) unitName else "${unitName}s"
        return "$value $label free trial"
    }

    private fun PurchasesException.toSubscriptionError(): SubscriptionError {
        val msg = message
        return when {
            msg.contains("network", ignoreCase = true) -> SubscriptionError.Network
            msg.contains("timeout", ignoreCase = true) -> SubscriptionError.Network
            else -> SubscriptionError.Unknown(msg)
        }
    }
}
