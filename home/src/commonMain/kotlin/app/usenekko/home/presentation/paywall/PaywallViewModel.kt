package app.usenekko.home.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.shared.domain.Result
import app.usenekko.shared.subscription.PurchaseOutcome
import app.usenekko.shared.subscription.SubscriptionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val subscriptionRepository: SubscriptionRepository,
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
                    _events.send(PaywallEvent.ShowError("Couldn't load plans. Please try again."))
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
                }
                is PurchaseOutcome.Error -> {
                    _state.update { it.copy(isPurchasing = false) }
                    _events.send(PaywallEvent.ShowError(outcome.message ?: "Purchase failed."))
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
                        _events.send(PaywallEvent.ShowError("No active subscription found to restore."))
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isRestoring = false) }
                    _events.send(PaywallEvent.ShowError("Restore failed. Try again."))
                }
            }
        }
    }
}
