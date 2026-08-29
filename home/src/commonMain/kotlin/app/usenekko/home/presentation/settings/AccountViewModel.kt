package app.usenekko.home.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.data.AccountRepository
import app.usenekko.home.data.HomeRepository
import app.usenekko.home.domain.toAccountErrorResource
import app.usenekko.home.domain.toUserMessageResource
import app.usenekko.home.domain.BadgeSlot
import app.usenekko.shared.domain.AccountProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

data class AccountState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: AccountProfile? = null,
    val fullName: String? = null,
    val createdAt: String? = null,
    val totalContacts: Int = 0,
    val totalCheckIns: Int = 0,
    val badgeSlots: List<BadgeSlot> = emptyList(),
    val isUpdatingAvatar: Boolean = false,
    val error: StringResource? = null,
)

class AccountViewModel(
    private val homeRepository: HomeRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        observeRepositories()
        load()
    }

    private fun observeRepositories() {
        viewModelScope.launch {
            combine(homeRepository.state, accountRepository.state) { home, account ->
                AccountState(
                    isLoading = home.snapshot == null || account.snapshot == null,
                    isRefreshing = home.isRefreshing || account.isRefreshing,
                    profile = account.snapshot?.profile,
                    fullName = account.snapshot?.profile?.resolvedName,
                    createdAt = account.snapshot?.profile?.createdAt,
                    totalContacts = home.snapshot?.contacts?.size ?: 0,
                    totalCheckIns = home.snapshot?.checkInHistory?.size ?: 0,
                    badgeSlots = account.snapshot?.badgeSlots.orEmpty(),
                    isUpdatingAvatar = account.isUpdatingAvatar,
                    error = account.error?.toAccountErrorResource() ?: home.error?.toUserMessageResource(),
                )
            }.collectLatest { _state.value = it }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            coroutineScope {
                launch { homeRepository.load(forceRefresh) }
                launch { accountRepository.load(forceRefresh) }
            }
        }
    }

    fun refreshIfStale() {
        load()
    }

    fun selectAvatar(index: Int) {
        viewModelScope.launch {
            accountRepository.updateSelectedAvatarId(index.toString())
        }
    }
}