package app.usefoster.home

import app.usefoster.shared.domain.Result
import app.usefoster.shared.subscription.PaywallOffering
import app.usefoster.shared.subscription.PaywallPackage
import app.usefoster.shared.subscription.PurchaseOutcome
import app.usefoster.shared.subscription.SubscriptionError
import app.usefoster.shared.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSubscriptionRepository(
    initialSubscribed: Boolean = false,
) : SubscriptionRepository {

    private val _isSubscribed = MutableStateFlow(initialSubscribed)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed

    private val _isCancellationPending = MutableStateFlow(false)
    override val isCancellationPending: StateFlow<Boolean> = _isCancellationPending

    private val _activeProductId = MutableStateFlow<String?>(if (initialSubscribed) "foster_premium_annual" else null)
    override val activeProductId: StateFlow<String?> = _activeProductId

    private val _hasBillingIssue = MutableStateFlow(false)
    override val hasBillingIssue: StateFlow<Boolean> = _hasBillingIssue

    fun setSubscribed(value: Boolean) {
        _isSubscribed.value = value
        if (!value) _activeProductId.value = null
    }

    override fun manageSubscriptionUrl(): String? =
        _activeProductId.value?.let { "https://play.google.com/store/account/subscriptions?sku=$it&package=app.usefoster" }

    override suspend fun refresh(): Result<Unit, SubscriptionError> = Result.Success(Unit)

    override suspend fun identify(userId: String?): Result<Unit, SubscriptionError> = Result.Success(Unit)

    override suspend fun loadPaywallOffering(offeringIdentifier: String?): Result<PaywallOffering, SubscriptionError> =
        Result.Error(SubscriptionError.NotConfigured)

    override suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome = PurchaseOutcome.Error

    override suspend fun restorePurchases(): Result<Boolean, SubscriptionError> = Result.Success(false)
}
