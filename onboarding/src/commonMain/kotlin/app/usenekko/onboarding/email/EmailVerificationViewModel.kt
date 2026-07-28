package app.usenekko.onboarding.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EmailVerificationViewModel(
    private val email: String,
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(EmailVerificationState())
    val state = _state.asStateFlow()

    private val _events = Channel<EmailVerificationEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.EmailVerification) }
    }

    fun onAction(action: EmailVerificationAction) {
        when (action) {
            is EmailVerificationAction.CodeChanged -> {
                _state.update { it.copy(code = action.code, isVerifying = false) }
            }
            is EmailVerificationAction.VerifyClicked -> verifyAndProceed()
            is EmailVerificationAction.Done -> verifyAndProceed()
            is EmailVerificationAction.BackClicked -> sendEvent(EmailVerificationEvent.NavigateBack)
            is EmailVerificationAction.SkipClicked -> sendEvent(EmailVerificationEvent.NavigateSkip)
        }
    }

    private fun verifyAndProceed() {
        _state.update { it.copy(isVerifying = true) }
        draftStore.update {
            it.copy(
                emailVerified = true,
                currentStep = OnboardingStep.Name,
            )
        }
        sendEvent(EmailVerificationEvent.NavigateToNext)
    }

    private fun sendEvent(event: EmailVerificationEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
