package app.usenekko.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.launch

class HomeViewModel(
    private val contactDataSource: ContactDataSource,
) : ViewModel() {

    init {
        viewModelScope.launch {
            when (val result = contactDataSource.getContacts()) {
                is Result.Success -> kotlin.io.println(
                    "HomeContacts: Success(${result.data.size}) -> ${result.data.map { it.name }}"
                )
                is Result.Error -> kotlin.io.println(
                    "HomeContacts: Error -> $result.error"
                )
            }
        }
    }
}
