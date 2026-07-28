package app.usenekko.onboarding.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.onboarding.domain.GroupDraft
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GroupViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _showCreateGroupSheet = MutableStateFlow(false)
    val showCreateGroupSheet = _showCreateGroupSheet.asStateFlow()

    private val _events = Channel<GroupEvent>()
    val events = _events.receiveAsFlow()

    init {
        draftStore.update { it.copy(currentStep = OnboardingStep.Group) }
    }

    fun onCreateGroupClicked() {
        _showCreateGroupSheet.value = true
    }

    fun onDismissCreateGroupSheet() {
        _showCreateGroupSheet.value = false
    }

    fun onSaveGroup(name: String) {
        val group = GroupDraft(
            id = "group_${Random.nextLong()}",
            name = name,
        )
        draftStore.update {
            it.copy(
                groups = listOf(group),
                currentStep = OnboardingStep.Group,
            )
        }
        _showCreateGroupSheet.value = false
    }

    fun onNextClicked() {
        draftStore.update { it.copy(currentStep = OnboardingStep.DayReminder) }
        sendEvent(GroupEvent.NavigateToNext)
    }

    fun onBackClicked() {
        sendEvent(GroupEvent.NavigateBack)
    }

    private fun sendEvent(event: GroupEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
