package app.usenekko.home.presentation.brainstorm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.BrainstormRepository
import app.usenekko.home.domain.BrainstormError
import app.usenekko.home.domain.BrainstormGeneration
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrainstormViewModel(
    private val contactId: String,
    private val repository: BrainstormRepository,
    private val contactDataSource: ContactDataSource? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(BrainstormState())
    val state: StateFlow<BrainstormState> = _state.asStateFlow()

    init {
        observeHistory()
        loadHistory()
        generate()
        loadContactPhoneNumber()
    }

    fun onAction(action: BrainstormAction) {
        when (action) {
            BrainstormAction.Generate -> generate()
            BrainstormAction.DismissNotice ->
                _state.value = _state.value.copy(notice = null, error = null)
            is BrainstormAction.ShowNotice ->
                _state.value = _state.value.copy(notice = action.message, error = null)
        }
    }

    /**
     * Resolves the contact's phone number (captured at import time) so
     * brainstorm cards can hand their content off to the SMS app. Phone is
     * optional — manual contacts have none and the UI shows a notice instead.
     */
    private fun loadContactPhoneNumber() {
        val dataSource = contactDataSource ?: return
        viewModelScope.launch {
            when (val result = dataSource.getContacts()) {
                is Result.Success -> {
                    val phone = result.data
                        .firstOrNull { it.id == contactId }
                        ?.phoneNumber
                    _state.value = _state.value.copy(contactPhoneNumber = phone)
                }
                is Result.Error -> Unit // phone is optional; never block brainstorm on this
            }
        }
    }

    fun refreshIfStale() {
        loadHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.state(contactId).collectLatest { repositoryState ->
                _state.value = _state.value.copy(
                    history = repositoryState.snapshot?.history.orEmpty(),
                    isLoadingHistory = repositoryState.snapshot == null,
                    isRefreshing = repositoryState.isRefreshing,
                    error = repositoryState.error?.let(::messageOf) ?: _state.value.error,
                )
            }
        }
    }

    private fun loadHistory(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            when (val result = repository.load(contactId, forceRefresh)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        history = result.data.history,
                        isLoadingHistory = false,
                        currentTopics = _state.value.currentTopics
                            ?: result.data.history.firstOrNull()?.topics,
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
            _state.value = _state.value.copy(
                isGenerating = true,
                error = null,
                notice = null,
            )
            when (val result = repository.generate(contactId)) {
                is Result.Success -> when (val generation = result.data) {
                    is BrainstormGeneration.Generated -> {
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            currentTopics = generation.topics,
                        )
                        // Invalidate and refresh history after the server mutation.
                        loadHistory(forceRefresh = true)
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
