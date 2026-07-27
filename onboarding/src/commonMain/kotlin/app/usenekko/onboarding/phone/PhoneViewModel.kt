package app.usenekko.onboarding.phone

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

class PhoneViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        PhoneState(
            phoneNumber = draftStore.draft.value.phoneNumber.removePrefix(CountryCode),
            isContinueEnabled = draftStore.draft.value.phoneNumber.removePrefix(CountryCode).isNotBlank(),
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<PhoneEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { draft ->
            draft.copy(currentStep = OnboardingStep.Phone)
        }
    }

    fun onAction(action: PhoneAction) {
        when (action) {
            is PhoneAction.PhoneNumberChanged -> {
                val digits = action.phoneNumber.filter(Char::isDigit).take(MaxPhoneDigits)
                _state.update {
                    it.copy(
                        phoneNumber = digits,
                        isContinueEnabled = digits.isNotBlank(),
                    )
                }
                draftStore.update { draft ->
                    draft.copy(
                        phoneNumber = "$CountryCode$digits",
                        currentStep = OnboardingStep.Phone,
                    )
                }
            }

            PhoneAction.ContinueClicked -> navigateToCodeVerification()
            PhoneAction.BackClicked -> sendEvent(PhoneEvent.NavigateBack)
            PhoneAction.SkipClicked -> sendEvent(PhoneEvent.NavigateSkip)
        }
    }

    private fun navigateToCodeVerification() {
        val phoneNumber = _state.value.phoneNumber
        if (phoneNumber.isBlank()) return

        draftStore.update { draft ->
            draft.copy(
                phoneNumber = "$CountryCode$phoneNumber",
                currentStep = OnboardingStep.CodeVerification,
            )
        }
        sendEvent(PhoneEvent.NavigateToCodeVerification("$CountryCode$phoneNumber"))
    }

    private fun sendEvent(event: PhoneEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    private companion object {
        const val CountryCode = "+60"
        const val MaxPhoneDigits = 10
    }
}
