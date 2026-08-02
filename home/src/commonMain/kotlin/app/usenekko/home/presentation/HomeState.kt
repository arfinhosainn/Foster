package app.usenekko.home.presentation

import app.usenekko.home.domain.Group

data class HomeState(
    val isLoading: Boolean = true,
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val totalContactCount: Int = 0,
    val outstandingCount: Int = 0,
    val upToDateCount: Int = 0,
    val error: String? = null,
)
