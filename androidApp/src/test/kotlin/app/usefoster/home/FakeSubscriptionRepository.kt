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

    fun setSubscribed(value: Boolean) {
        _isSubscribed.value = value
    }

    override suspend fun refresh(): Result<Unit, SubscriptionError> = Result.Success(Unit)

    override suspend fun loadPaywallOffering(): Result<PaywallOffering, SubscriptionError> =
        Result.Error(SubscriptionError.NotConfigured)

    override suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome = PurchaseOutcome.Error

    override suspend fun restorePurchases(): Result<Boolean, SubscriptionError> = Result.Success(false)
}
