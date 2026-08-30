package app.usefoster.home.presentation.brainstorm

import app.usefoster.home.domain.BrainstormSession
import app.usefoster.home.domain.BrainstormTopic
import org.jetbrains.compose.resources.StringResource

enum class BrainstormTab { CurrentOutput, History }

sealed interface BrainstormAction {
    data object Generate : BrainstormAction
    data object DismissNotice : BrainstormAction

    /** Surfaces a dismissible banner with a pre-localized message. */
    data class ShowNotice(val message: String) : BrainstormAction
}

data class BrainstormState(
    val isGenerating: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: StringResource? = null,
    val notice: String? = null,
    val currentTopics: List<BrainstormTopic>? = null,
    val history: List<BrainstormSession> = emptyList(),
    val isLoadingHistory: Boolean = true,
    /** Phone number of the contact being brainstormed for; enables SMS hand-off. */
    val contactPhoneNumber: String? = null,
)
