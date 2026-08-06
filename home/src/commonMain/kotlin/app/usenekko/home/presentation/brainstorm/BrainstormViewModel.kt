package app.usenekko.home.presentation.brainstorm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.BrainstormDataSource
import app.usenekko.home.domain.BrainstormError
import app.usenekko.home.domain.BrainstormGeneration
import app.usenekko.shared.domain.Result
import app.usenekko.shared.subscription.GateResult
import app.usenekko.shared.subscription.SubscriptionGates
import app.usenekko.shared.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrainstormViewModel(
    private val contactId: String,
    private val dataSource: BrainstormDataSource,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrainstormState())
    val state: StateFlow<BrainstormState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun onAction(action: BrainstormAction) {
        when (action) {
            BrainstormAction.Generate -> generate()
            BrainstormAction.DismissNotice ->
                _state.value = _state.value.copy(notice = null, error = null)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            when (val result = dataSource.getHistory(contactId)) {
                is Result.Success -> {
                    val history = result.data
                    _state.value = _state.value.copy(
                        history = history,
                        isLoadingHistory = false,
                        currentTopics = _state.value.currentTopics
                            ?: history.firstOrNull()?.topics,
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingHistory = false,
                        error = messageOf(result.error),
                    )
                }
            }
        }
    }

    private fun generate() {
        if (_state.value.isGenerating) return
        viewModelScope.launch {
            // Gate 2 — Brainstorming: free users get 3 generations per calendar month.
            // Block + show paywall when the monthly limit is exceeded.
            val isSubscribed = subscriptionRepository.isSubscribed.value
            if (!isSubscribed) {
                when (val countResult = dataSource.getMonthlyGenerationCount()) {
                    is Result.Success -> {
                        val gate = SubscriptionGates.brainstormGate(
                            isSubscribed = isSubscribed,
                            monthlyGenerationCount = countResult.data,
                        )
                        if (gate is GateResult.Blocked) {
                            _state.value = _state.value.copy(
                                isGenerating = false,
                                paywallReason = gate.reason,
                            )
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        // Can't check count — proceed cautiously (don't block on error).
                    }
                }
            }

            _state.value = _state.value.copy(
                isGenerating = true,
                error = null,
                notice = null,
                paywallReason = null,
            )
            when (val result = dataSource.generate(contactId)) {
                is Result.Success -> when (val generation = result.data) {
                    is BrainstormGeneration.Generated -> {
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            currentTopics = generation.topics,
                        )
                        // Refresh History + cooldown state from the server.
                        loadHistory()
                    }
                    is BrainstormGeneration.Cooldown -> {
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            notice = "Already generated today. Check back tomorrow for fresh ideas.",
                        )
                    }
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isGenerating = false,
                        error = messageOf(result.error),
                    )
                }
            }
        }
    }

    private fun messageOf(error: BrainstormError): String = when (error) {
        is BrainstormError.Network -> "Network error. Check your connection and try again."
        is BrainstormError.NotAuthenticated -> "Your session expired. Please sign in again."
        is BrainstormError.Unknown -> error.detail?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Please try again."
    }
}
