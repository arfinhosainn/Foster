package app.usenekko.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.shared.domain.Result
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class HomeViewModel(
    private val contactDataSource: ContactDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = contactDataSource.getContacts()) {
                is Result.Success -> {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    _state.value = HomeState(
                        isLoading = false,
                        outstandingCount = result.data.count { it.isOutstanding(today) },
                        upToDateCount = result.data.count { !it.isOutstanding(today) },
                    )
                }
                is Result.Error -> {
                    _state.value = HomeState(
                        isLoading = false,
                        error = result.error.toString(),
                    )
                }
            }
        }
    }

    private fun Contact.isOutstanding(today: LocalDate): Boolean {
        val next = nextCheckInDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return true
        return next <= today
    }
}
