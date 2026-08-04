package app.usenekko.home.domain

import app.usenekko.shared.domain.Result

data class BrainstormTopic(
    val id: String? = null,
    val icon: String? = null,
    val title: String,
    val description: String? = null,
)

data class BrainstormSession(
    val id: String,
    val createdAt: String,
    val topics: List<BrainstormTopic>,
)

/**
 * Result of a brainstorm "generate" call. Generation always produces a fresh
 * batch (the button is the generate action, not just a view), and the server
 * enforces a once-per-contact-per-day cooldown.
 */
sealed interface BrainstormGeneration {
    data class Generated(val topics: List<BrainstormTopic>) : BrainstormGeneration
    data object Cooldown : BrainstormGeneration
}

interface BrainstormDataSource {
    suspend fun generate(contactId: String): Result<BrainstormGeneration, BrainstormError>
    suspend fun getHistory(contactId: String): Result<List<BrainstormSession>, BrainstormError>
}

sealed interface BrainstormError {
    data object NotAuthenticated : BrainstormError
    data object Network : BrainstormError
    data class Unknown(val detail: String?) : BrainstormError
}
