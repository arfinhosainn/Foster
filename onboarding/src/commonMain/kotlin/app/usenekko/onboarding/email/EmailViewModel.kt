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

class EmailViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        EmailState(
            email = draftStore.draft.value.email,
            isContinueEnabled = draftStore.draft.value.email.isNotBlank(),
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<EmailEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.Email) }
    }

    fun onAction(action: EmailAction) {
        when (action) {
            is EmailAction.EmailChanged -> {
                val email = action.email.filter { it != ' ' }
                _state.update {
                    it.copy(email = email, isContinueEnabled = email.isNotBlank())
                }
                draftStore.update {
                    it.copy(email = email, currentStep = OnboardingStep.Email)
                }
            }
            EmailAction.ContinueClicked -> navigateToEmailVerification()
            EmailAction.BackClicked -> sendEvent(EmailEvent.NavigateBack)
            EmailAction.SkipClicked -> sendEvent(EmailEvent.NavigateSkip)
        }
    }

    private fun navigateToEmailVerification() {
        val email = _state.value.email
        if (email.isBlank()) return

        draftStore.update {
            it.copy(email = email, currentStep = OnboardingStep.EmailVerification)
        }
        sendEvent(EmailEvent.NavigateToEmailVerification(email))
    }

    private fun sendEvent(event: EmailEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
