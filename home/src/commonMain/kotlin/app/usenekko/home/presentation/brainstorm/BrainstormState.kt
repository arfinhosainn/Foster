package app.usenekko.home.presentation.brainstorm

import app.usenekko.home.domain.BrainstormSession
import app.usenekko.home.domain.BrainstormTopic

enum class BrainstormTab { CurrentOutput, History }

sealed interface BrainstormAction {
    data object Generate : BrainstormAction
    data object DismissNotice : BrainstormAction
}

data class BrainstormState(
    val isGenerating: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val currentTopics: List<BrainstormTopic>? = null,
    val history: List<BrainstormSession> = emptyList(),
    val isLoadingHistory: Boolean = true,
)
