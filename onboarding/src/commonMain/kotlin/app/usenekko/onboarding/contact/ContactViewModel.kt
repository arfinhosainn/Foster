package app.usenekko.onboarding.contact

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

class ContactViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ContactState(
            contactName = draftStore.draft.value.contactName,
            selectedAvatarIndex = draftStore.draft.value.selectedAvatarId?.toIntOrNull(),
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
                _state.update { it.copy(contactName = action.value) }
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
            is ContactAction.NextClicked -> {
                draftStore.update { it.copy(currentStep = OnboardingStep.Group) }
                sendEvent(ContactEvent.NavigateToNext)
            }
            is ContactAction.BackClicked -> sendEvent(ContactEvent.NavigateBack)
            is ContactAction.SkipClicked -> {
                draftStore.update { it.copy(currentStep = OnboardingStep.Group) }
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
