package app.usefoster.shared.subscription

import app.usefoster.shared.domain.Result
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitLogIn
import com.revenuecat.purchases.kmp.ktx.awaitLogOut
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.DiscountPaymentMode
import com.revenuecat.purchases.kmp.models.EntitlementInfo
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.models.freePhase
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal fun isCancellationPending(
    isActive: Boolean,
    unsubscribeDetectedAtMillis: Long?,
): Boolean = isActive && unsubscribeDetectedAtMillis != null

/**
 * RevenueCat-backed [SubscriptionRepository]. A single `unlimited` entitlement
 * unlocks every gated feature (contacts + brainstorming).
 *
 * Entitlement state ([isSubscribed]) is a hot [StateFlow] that updates
 * immediately after a purchase or restore — no app restart needed — so whatever
 * gate triggered the paywall unblocks the instant the purchase succeeds.
 *
 * Fail-open: [isSubscribed] only ever flips to false from an AUTHORITATIVE
 * [CustomerInfo] that says the entitlement lapsed. A failed [refresh]
 * ([SubscriptionError.Network] / offline) keeps the last known value — and the
 * last known value is ALSO seeded from [EntitlementSnapshotStore] at login, so
 * a paying customer on a flaky connection is never locked out of what they pay
 * for; the first successful refresh overwrites the snapshot.
 */
class RevenueCatSubscriptionRepository(
    private val snapshotStore: EntitlementSnapshotStore,
    backgroundDispatcher: CoroutineContext = Dispatchers.Default,
) : SubscriptionRepository {

    private val scope = CoroutineScope(SupervisorJob() + backgroundDispatcher)

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _isCancellationPending = MutableStateFlow(false)
    override val isCancellationPending: StateFlow<Boolean> = _isCancellationPending.asStateFlow()

    private val _activeProductId = MutableStateFlow<String?>(null)
    override val activeProductId: StateFlow<String?> = _activeProductId.asStateFlow()

    private val _hasBillingIssue = MutableStateFlow(false)
    override val hasBillingIssue: StateFlow<Boolean> = _hasBillingIssue.asStateFlow()

    /**
     * Platform-correct management URL from the latest [CustomerInfo] (Google
     * Play subscriptions page on Android, App Store subscriptions on iOS).
     * Null when the user has no active subscription or none has been fetched.
     */
    private var managementUrl: String? = null

    // RC Package objects keyed by identifier, cached when the offering is loaded
    // so purchase() can resolve the UI-facing PaywallPackage back to the real
    // RC Package that awaitPurchase() needs.
    private var cachedPackages: Map<String, Package> = emptyMap()

    private var currentUserId: String? = null

    private val revenueCatDelegate = object : PurchasesDelegate {
        override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
            updateFromCustomerInfo(customerInfo)
        }

        override fun onPurchasePromoProduct(
            product: StoreProduct,
            startPurchase: (
                onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
            ) -> Unit,
        ) {
            // Promotional App Store purchases are not initiated by this paywall.
        }
    }

    override suspend fun identify(userId: String?): Result<Unit, SubscriptionError> {
        if (!Purchases.isConfigured) return Result.Error(SubscriptionError.NotConfigured)
        return try {
            if (userId.isNullOrBlank()) {
                if (!Purchases.sharedInstance.isAnonymous) {
                    Purchases.sharedInstance.awaitLogOut()
                }
                currentUserId = null
                _isSubscribed.value = false
                _isCancellationPending.value = false
                _activeProductId.value = null
                _hasBillingIssue.value = false
                managementUrl = null
            } else if (currentUserId != userId) {
                if (!Purchases.sharedInstance.isAnonymous) {
                    Purchases.sharedInstance.awaitLogOut()
                }
                Purchases.sharedInstance.awaitLogIn(userId)
                currentUserId = userId
                // Fail-open seed: before the authoritative refresh, load this
                // user's last known entitlement. If refresh() then fails
                // (airplane, tunnel, RC unreachable) the gates keep the user
                // unlocked instead of locking a paying customer out.
                snapshotStore.load(userId)?.let { snapshot ->
                    _isSubscribed.value = snapshot.subscribed
                    _isCancellationPending.value = snapshot.cancellationPending
                    _activeProductId.value = snapshot.productId
                    managementUrl = snapshot.managementUrl
                }
                refresh()
            }
            Result.Success(Unit)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            when (error) {
                is PurchasesException -> Result.Error(error.toSubscriptionError())
                else -> Result.Error(SubscriptionError.Unknown(error.message))
            }
        }
    }

    override suspend fun refresh(): Result<Unit, SubscriptionError> {
        if (!Purchases.isConfigured) return Result.Error(SubscriptionError.NotConfigured)
        installDelegate()
        try {
            val info = Purchases.sharedInstance.awaitCustomerInfo()
            updateFromCustomerInfo(info)
            return Result.Success(Unit)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Keep last-known state on transient errors.
            return when (error) {
                is PurchasesException -> Result.Error(error.toSubscriptionError())
                else -> Result.Error(SubscriptionError.Unknown(null))
            }
        }
    }

    override suspend fun loadPaywallOffering(
        offeringIdentifier: String?,
    ): Result<PaywallOffering, SubscriptionError> {
        if (!Purchases.isConfigured) return Result.Error(SubscriptionError.NotConfigured)
        installDelegate()
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val current = offeringIdentifier?.let { offerings[it] } ?: offerings.current
                ?: return Result.Error(SubscriptionError.NotConfigured)
            Result.Success(current.toPaywallOffering())
        } catch (e: PurchasesException) {
            Result.Error(e.toSubscriptionError())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(SubscriptionError.Unknown(e.message))
        }
    }

    override suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome {
        if (!Purchases.isConfigured) return PurchaseOutcome.Error
        installDelegate()
        val rcPackage = cachedPackages[pkg.identifier]
            ?: return PurchaseOutcome.Error
        return try {
            Purchases.sharedInstance.awaitPurchase(rcPackage)
            PurchaseOutcome.Success
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) PurchaseOutcome.Cancelled
            else PurchaseOutcome.Error
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            PurchaseOutcome.Error
        }
    }

    override suspend fun restorePurchases(): Result<Boolean, SubscriptionError> {
        if (!Purchases.isConfigured) return Result.Error(SubscriptionError.NotConfigured)
        installDelegate()
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            updateFromCustomerInfo(info)
            Result.Success(isUnlimitedActive(info))
        } catch (e: PurchasesException) {
            Result.Error(e.toSubscriptionError())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(SubscriptionError.Unknown(e.message))
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private fun installDelegate() {
        Purchases.sharedInstance.delegate = revenueCatDelegate
    }

    /**
     * Single sync point for everything derived from [CustomerInfo]: the
     * entitlement flag, cancellation state, active product id and store URL.
     * Called from the delegate, [refresh] and [restorePurchases] so every
     * entitlement change (purchase, restore, cancellation, expiry) lands in
     * the same subscription state fields.
     */
    private fun updateFromCustomerInfo(info: CustomerInfo) {
        val active = isUnlimitedActive(info)
        _isSubscribed.value = active
        _isCancellationPending.value = active && isCancellationPending(unlimitedEntitlement(info))
        managementUrl = info.managementUrlString
        _activeProductId.value = if (active) {
            // Google Play product ids are "<subscriptionId>:<basePlanId>"; the
            // sku expected by play.google.com/.../subscriptions is the
            // subscription id before the ':'.
            info.activeSubscriptions.firstOrNull()?.substringBefore(":")
        } else {
            null
        }
        // A non-null billingIssue date means the renewal payment failed and the
        // store is mid-retry / grace. The entitlement stays ACTIVE during the
        // grace period, so isSubscribed above remains true; we track it anyway
        // so that when grace ends (isSubscribed -> false) we can deep-link the
        // user to fix their payment method instead of pitching a new purchase.
        _hasBillingIssue.value = unlimitedEntitlement(info)?.billingIssueDetectedAtMillis != null

        // Persist the last-known state per user so a future offline cold start
        // can fail open. Only real CustomerInfo results write — never sign-outs.
        currentUserId?.let { userId ->
            scope.launch {
                snapshotStore.save(
                    userId,
                    EntitlementSnapshot(
                        subscribed = active,
                        cancellationPending = _isCancellationPending.value,
                        productId = _activeProductId.value,
                        managementUrl = managementUrl,
                    ),
                )
            }
        }
    }

    private fun unlimitedEntitlement(info: CustomerInfo): EntitlementInfo? =
        info.entitlements.all[UNLIMITED_ENTITLEMENT_ID]

    private fun isCancellationPending(entitlement: EntitlementInfo?): Boolean =
        isCancellationPending(
            isActive = entitlement?.isActive == true,
            unsubscribeDetectedAtMillis = entitlement?.unsubscribeDetectedAtMillis,
        )

    override fun manageSubscriptionUrl(): String? {
        // RevenueCat's platform-correct management page (Google Play on
        // Android, App Store on iOS) — derived from the latest CustomerInfo.
        managementUrl?.let { return it }

        // Fallback: build Google Play's native subscriptions page for the
        // active product exactly as Play documents it. One tap from Settings
        // opens it; one more tap cancels (the two-tap rule).
        val sku = _activeProductId.value
        if (sku != null) {
            return "https://play.google.com/store/account/subscriptions?sku=$sku&package=app.usefoster"
        }

        // Last resort: the bare subscriptions page (reaches the payment-method
        // fix surface even when the product id isn't known, e.g. billing issue
        // after grace ended). Only surfaced for users with an active
        // subscription or a billing issue.
        return if (_hasBillingIssue.value) {
            "https://play.google.com/store/account/subscriptions?package=app.usefoster"
        } else {
            null
        }
    }

    /**
     * RevenueCat treats an entitlement as ACTIVE while a subscription is in its
     * grace period or on account hold (renewal paid late or billing failed);
     * only once that window passes does the entitlement go inactive. Reading
     * only the *active* map therefore already keeps paying customers unlocked
     * through a payment snag — exactly what we want.
     */
    private fun isUnlimitedActive(info: CustomerInfo): Boolean =
        info.entitlements.active[UNLIMITED_ENTITLEMENT_ID] != null

    private fun Offering.toPaywallOffering(): PaywallOffering {
        val monthlyPackage = monthly
        val annualPackage = annual
        val packages = mutableMapOf<String, Package>()
        monthlyPackage?.let { packages[cacheKey(it)] = it }
        annualPackage?.let { packages[cacheKey(it)] = it }
        cachedPackages = cachedPackages + packages
        return PaywallOffering(
            monthly = monthlyPackage?.toPaywallPackage(BillingPeriod.MONTHLY, cacheKey(monthlyPackage)),
            annual = annualPackage?.toPaywallPackage(BillingPeriod.ANNUAL, cacheKey(annualPackage)),
        )
    }

    private fun Offering.cacheKey(pkg: Package): String = "$identifier:${pkg.identifier}"

    private fun Package.toPaywallPackage(period: BillingPeriod, cacheKey: String): PaywallPackage {
        val product = storeProduct
        val (hasTrial, trialString) = product.freeTrialInfo()
        return PaywallPackage(
            identifier = cacheKey,
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
        val (displayValue, unitName) = if (unit.name.equals("WEEK", ignoreCase = true)) {
            value * 7 to "day"
        } else {
            value to unit.name.lowercase()
        }
        val label = if (displayValue == 1) unitName else "${unitName}s"
        return "$displayValue-$label free trial"
    }

    private fun PurchasesException.toSubscriptionError(): SubscriptionError {
        val msg = message
        return when {
            msg.contains("network", ignoreCase = true) -> SubscriptionError.Network
            msg.contains("timeout", ignoreCase = true) -> SubscriptionError.Network
            else -> SubscriptionError.Unknown(null)
        }
    }
}
