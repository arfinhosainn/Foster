package app.usenekko.onboarding.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.shared.domain.Result
import app.usenekko.onboarding.domain.toUserMessage
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val draftStore: OnboardingDraftStore,
    private val profileDataSource: OnboardingProfileDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationState())
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    private val _events = Channel<NotificationEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: NotificationAction) {
        when (action) {
            NotificationAction.TurnOnClicked -> Unit
            is NotificationAction.PermissionStateChanged -> {
                _state.value = _state.value.copy(isNotificationEnabled = action.enabled)
            }
            is NotificationAction.PermissionResult -> completeOnboarding(
                notificationPermissionAsked = true,
                notificationPermissionGranted = action.granted,
            )
            is NotificationAction.SkipClicked -> {
                completeOnboarding(
                    notificationPermissionAsked = false,
                    notificationPermissionGranted = false,
                )
            }
        }
    }

    private fun completeOnboarding(
        notificationPermissionAsked: Boolean,
        notificationPermissionGranted: Boolean,
    ) {
        _state.value = _state.value.copy(isNotificationEnabled = notificationPermissionGranted)
        val completedDraft = draftStore.draft.value.copy(
            notificationPermissionAsked = notificationPermissionAsked,
            notificationPermissionGranted = notificationPermissionGranted,
            currentStep = OnboardingStep.Complete,
        )
        draftStore.update { completedDraft }
        submitOnboarding(completedDraft)
    }

    private fun submitOnboarding(draft: OnboardingDraft) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true)
            when (val result = profileDataSource.submitOnboarding(draft)) {
                is Result.Success -> {
                    draftStore.clear()
                    _state.value = _state.value.copy(isSubmitting = false)
                    _events.send(NotificationEvent.NavigateToMainApp)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(isSubmitting = false)
                    _events.send(NotificationEvent.ShowError(result.error.toUserMessage()))
                }
            }
        }
    }
}
