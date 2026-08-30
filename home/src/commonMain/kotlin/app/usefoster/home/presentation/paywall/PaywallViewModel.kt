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

class PaywallViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val paywallGateManager: PaywallGateManager? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    private val _events = Channel<PaywallEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadOffering()
    }

    fun onAction(action: PaywallAction) {
        when (action) {
            is PaywallAction.SelectPeriod -> _state.update { it.copy(selectedPeriod = action.period) }
            PaywallAction.Purchase -> purchase()
            PaywallAction.Restore -> restore()
        }
    }

    private fun loadOffering() {
        viewModelScope.launch {
            when (val result = subscriptionRepository.loadPaywallOffering()) {
                is Result.Success -> _state.update {
                    it.copy(isLoading = false, offering = result.data)
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(isLoading = false)
                    }
                    _events.send(PaywallEvent.ShowError(result.error.toUserMessageResource()))
                }
            }
        }
    }

    private fun purchase() {
        val pkg = _state.value.selectedPackage ?: return
        if (_state.value.isPurchasing) return
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true) }
            when (val outcome = subscriptionRepository.purchase(pkg)) {
                PurchaseOutcome.Success -> {
                    _state.update { it.copy(isPurchasing = false) }
                    _events.send(PaywallEvent.Subscribed)
                }
                PurchaseOutcome.Cancelled -> {
                    _state.update { it.copy(isPurchasing = false) }
                    // Backed out of checkout — report it so the gate engine can
                    // decide whether this moment earns the discount impression.
                    paywallGateManager?.let { manager ->
                        viewModelScope.launch { manager.reportTrigger(PaywallTrigger.ABANDONED_CHECKOUT) }
                    }
                }
                is PurchaseOutcome.Error -> {
                    _state.update { it.copy(isPurchasing = false) }
                    _events.send(PaywallEvent.ShowError(Res.string.paywall_purchase_failed))
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
                        _events.send(PaywallEvent.Subscribed)
                    } else {
                        _events.send(PaywallEvent.ShowError(Res.string.paywall_restore_none))
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isRestoring = false) }
                    _events.send(PaywallEvent.ShowError(result.error.toUserMessageResource()))
                }
            }
        }
    }
}
