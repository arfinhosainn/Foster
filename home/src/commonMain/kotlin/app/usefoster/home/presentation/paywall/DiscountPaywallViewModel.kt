package app.usefoster.home.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usefoster.shared.domain.Result
import app.usefoster.shared.paywall.PaywallGateManager
import app.usefoster.shared.paywall.PaywallTrigger
import app.usefoster.shared.subscription.PurchaseOutcome
import app.usefoster.shared.subscription.SubscriptionRepository
import app.usefoster.shared.subscription.toUserMessageResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import foster.home.generated.resources.Res
import foster.home.generated.resources.paywall_purchase_failed
import foster.home.generated.resources.paywall_restore_failed
import foster.home.generated.resources.paywall_restore_none

class DiscountPaywallViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val paywallGateManager: PaywallGateManager? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscountPaywallState())
    val state: StateFlow<DiscountPaywallState> = _state.asStateFlow()

    private val _events = Channel<DiscountPaywallEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Real countdown deadline from the gate engine — survives remounts. */
    val offerExpiresAtMillis: StateFlow<Long?> =
        paywallGateManager?.offerExpiresAtMillis
            ?: MutableStateFlow(null)

    init {
        loadOffering()
    }

    fun onAction(action: DiscountPaywallAction) {
        when (action) {
            DiscountPaywallAction.Purchase -> purchase()
            DiscountPaywallAction.Restore -> restore()
        }
    }

    private fun loadOffering() {
        viewModelScope.launch {
            when (val result = subscriptionRepository.loadPaywallOffering()) {
                is Result.Success -> _state.update {
                    it.copy(isLoading = false, annual = result.data?.annual)
                }
                is Result.Error -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun purchase() {
        val pkg = _state.value.annual ?: return
        if (_state.value.isPurchasing) return
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true) }
            when (val outcome = subscriptionRepository.purchase(pkg)) {
                PurchaseOutcome.Success -> {
                    _state.update { it.copy(isPurchasing = false) }
                    _events.send(DiscountPaywallEvent.Subscribed)
                }
                // Backed out of the native sheet mid-offer — strong intent signal.
                PurchaseOutcome.Cancelled -> {
                    _state.update { it.copy(isPurchasing = false) }
                    reportAbandonedCheckout()
                }
                is PurchaseOutcome.Error -> {
                    _state.update { it.copy(isPurchasing = false) }
                    _events.send(DiscountPaywallEvent.ShowError(Res.string.paywall_purchase_failed))
                }
            }
        }
    }

    private fun restore() {
        if (_state.value.isRestoring) return
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true) }
            when (val result = subscriptionRepository.restorePurchases()) {
                is Result.Success -> {
                    _state.update { it.copy(isRestoring = false) }
                    if (result.data) {
                        _events.send(DiscountPaywallEvent.Subscribed)
                    } else {
                        _events.send(DiscountPaywallEvent.ShowError(Res.string.paywall_restore_none))
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isRestoring = false) }
                    _events.send(DiscountPaywallEvent.ShowError(result.error.toUserMessageResource()))
                }
            }
        }
    }

    private fun reportAbandonedCheckout() {
        val manager = paywallGateManager ?: return
        viewModelScope.launch {
            manager.reportTrigger(PaywallTrigger.ABANDONED_CHECKOUT)
        }
    }

    override fun onCleared() {
        paywallGateManager?.consumeShow()
        super.onCleared()
    }
}
