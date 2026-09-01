package app.usefoster.onboarding.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ContactState(
            contactName = draftStore.draft.value.contactName,
            // A default avatar (index 0) is always pre-selected so the user
            // can just continue without touching the picker; they only need
            // to open it when they want a different avatar.
            selectedAvatarIndex = draftStore.draft.value.selectedAvatarId?.toIntOrNull() ?: 0,
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<ContactEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.Contact) }
    }

    fun onAction(action: ContactAction) {
        when (action) {
            is ContactAction.ContactNameChanged -> {
                _state.update {
                    it.copy(
                        contactName = action.value,
                        // Error clears as soon as the user starts typing again;
                        // it only re-appears on a failed Next attempt.
                        showNameError = action.value.isBlank() && it.showNameError,
                    )
                }
                draftStore.update {
                    it.copy(
                        contactName = action.value,
                        currentStep = OnboardingStep.Contact,
                    )
                }
            }
            is ContactAction.AvatarSelected -> {
                _state.update {
                    it.copy(
                        selectedAvatarIndex = action.index,
                        importedPhoto = null,
                        showAvatarPicker = false,
                    )
                }
                draftStore.update {
                    it.copy(
                        selectedAvatarId = action.index.toString(),
                        profilePhotoUri = null,
                        currentStep = OnboardingStep.Contact,
                    )
                }
            }
            is ContactAction.ContactImported -> {
                _state.update {
                    it.copy(
                        contactName = action.contact.name,
                        selectedAvatarIndex = if (action.contact.photo != null) null else it.selectedAvatarIndex,
                        importedPhoto = action.contact.photo,
                        showNameError = action.contact.name.isBlank(),
                    )
                }
                draftStore.update {
                    it.copy(
                        contactName = action.contact.name,
                        selectedAvatarId = if (action.contact.photo != null) null else it.selectedAvatarId,
                        profilePhotoUri = if (action.contact.photo != null) null else it.profilePhotoUri,
                        currentStep = OnboardingStep.Contact,
                    )
                }
            }
            ContactAction.ImportFailed -> sendEvent(ContactEvent.ImportFailed)
            is ContactAction.NextClicked -> {
                if (_state.value.contactName.isBlank()) {
                    // Inline validation: mark the field, no snackbar. The Next
                    // button is disabled anyway — this covers keyboard "Done".
                    _state.update { it.copy(showNameError = true) }
                } else {
                    _state.update { it.copy(showNameError = false) }
                    draftStore.update {
                        it.copy(
                            currentStep = OnboardingStep.Group,
                            // Persist the (possibly default) avatar selection so
                            // continuing without touching the picker still uses a
                            // real avatar. Imported photos keep a null avatar id.
                            selectedAvatarId = _state.value.selectedAvatarIndex?.toString()
                                ?: it.selectedAvatarId,
                        )
                    }
                    sendEvent(ContactEvent.NavigateToNext)
                }
            }
            is ContactAction.BackClicked -> sendEvent(ContactEvent.NavigateBack)
            is ContactAction.SkipClicked -> {
                draftStore.update {
                    it.copy(
                        currentStep = OnboardingStep.Group,
                        selectedAvatarId = _state.value.selectedAvatarIndex?.toString()
                            ?: it.selectedAvatarId,
                    )
                }
                sendEvent(ContactEvent.NavigateSkip)
            }
        }
    }

    fun onShowAvatarPicker() {
        _state.update { it.copy(showAvatarPicker = true) }
    }

    fun onDismissAvatarPicker() {
        _state.update { it.copy(showAvatarPicker = false) }
    }

    private fun sendEvent(event: ContactEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
