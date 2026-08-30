package app.usefoster.onboarding.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class NameViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _name = MutableStateFlow(draftStore.draft.value.name)
    val name = _name.asStateFlow()

    private val _events = Channel<NameEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.Name) }
    }

    fun onNameChanged(value: String) {
        _name.value = value
        draftStore.update {
            it.copy(name = value, currentStep = OnboardingStep.Name)
        }
    }

    fun onContinueClicked() {
        if (_name.value.isBlank()) {
            sendEvent(NameEvent.NameRequired)
            return
        }
        draftStore.update { it.copy(currentStep = OnboardingStep.Contact) }
        sendEvent(NameEvent.NavigateToNext)
    }

    fun onBackClicked() {
        sendEvent(NameEvent.NavigateBack)
    }

    fun onSkipClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.Contact) }
        sendEvent(NameEvent.NavigateSkip)
    }

    private fun sendEvent(event: NameEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
