package app.usenekko.home.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.ContactDataSource
import app.usenekko.shared.domain.AccountProfile
import app.usenekko.shared.domain.ProfileDataSource
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val EPOCH_START = "1970-01-01"
private const val EPOCH_END = "2999-12-31"

data class AccountState(
    val isLoading: Boolean = true,
    val profile: AccountProfile? = null,
    val fullName: String? = null,
    val createdAt: String? = null,
    val totalContacts: Int = 0,
    val totalCheckIns: Int = 0,
    val error: String? = null,
)

class AccountViewModel(
    private val profileDataSource: ProfileDataSource,
    private val contactDataSource: ContactDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = runCatching {
                coroutineScope {
                    val profile = async { profileDataSource.getProfile() }
                    val contacts = async { contactDataSource.getContacts() }
                    // Total check-ins across ALL contacts — derived from the
                    // existing check_ins rows (same pattern as the profile
                    // screen's checkInCount), no denormalized column.
                    val checkIns = async {
                        contactDataSource.getCheckIns(null, EPOCH_START, EPOCH_END)
                    }
                    Triple(profile.await(), contacts.await(), checkIns.await())
                }
            }

            result.fold(
                onSuccess = { (profile, contacts, checkIns) ->
                    val profileData = (profile as? Result.Success)?.data
                    val contactList = (contacts as? Result.Success)?.data.orEmpty()
                    val checkInCount = (checkIns as? Result.Success)?.data?.size ?: 0
                    _state.value = AccountState(
                        isLoading = false,
                        profile = profileData,
                        fullName = profileData?.resolvedName,
                        createdAt = profileData?.createdAt,
                        totalContacts = contactList.size,
                        totalCheckIns = checkInCount,
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message,
                    )
                },
            )
        }
    }
}