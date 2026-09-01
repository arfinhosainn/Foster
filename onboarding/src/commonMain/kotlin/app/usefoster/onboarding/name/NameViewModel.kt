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
    private val validateName: ValidateName = ValidateName(),
) : ViewModel() {
    private val _name = MutableStateFlow(draftStore.draft.value.name)
    val name = _name.asStateFlow()

    private val _showNameError = MutableStateFlow(false)
    val showNameError = _showNameError.asStateFlow()

    private val _events = Channel<NameEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.Name) }
    }

    fun onNameChanged(value: String) {
        _name.value = value
        // Error clears as soon as the user starts typing again; it only
        // re-appears on a failed Continue attempt.
        _showNameError.value = value.isBlank() && _showNameError.value
        draftStore.update {
            it.copy(name = value, currentStep = OnboardingStep.Name)
        }
    }

    fun onContinueClicked() {
        if (!validateName.validate(_name.value)) {
            // Inline validation: mark the field, no snackbar. The Continue
            // button is disabled while the name is blank — this covers the
            // too-short / too-long cases via keyboard "Done".
            _showNameError.value = true
            return
        }
        _showNameError.value = false
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
