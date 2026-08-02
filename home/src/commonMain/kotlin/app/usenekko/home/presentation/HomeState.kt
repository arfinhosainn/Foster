package app.usenekko.home.presentation

data class HomeState(
    val isLoading: Boolean = true,
    val outstandingCount: Int = 0,
    val upToDateCount: Int = 0,
    val error: String? = null,
)
