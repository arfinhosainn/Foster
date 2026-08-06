package app.usenekko.home

import app.usenekko.shared.domain.Result
import app.usenekko.shared.subscription.PaywallOffering
import app.usenekko.shared.subscription.PaywallPackage
import app.usenekko.shared.subscription.PurchaseOutcome
import app.usenekko.shared.subscription.SubscriptionError
import app.usenekko.shared.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSubscriptionRepository(
    initialSubscribed: Boolean = false,
) : SubscriptionRepository {

    private val _isSubscribed = MutableStateFlow(initialSubscribed)
    override val isSubscribed: StateFlow<Boolean> = _isSubscribed

    fun setSubscribed(value: Boolean) {
        _isSubscribed.value = value
    }

    override suspend fun refresh() {}

    override suspend fun loadPaywallOffering(): Result<PaywallOffering, SubscriptionError> =
        Result.Error(SubscriptionError.NotConfigured)

    override suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome = PurchaseOutcome.Error("not implemented")

    override suspend fun restorePurchases(): Result<Boolean, SubscriptionError> = Result.Success(false)
}
