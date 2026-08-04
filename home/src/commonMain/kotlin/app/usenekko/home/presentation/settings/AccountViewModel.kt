package app.usenekko.home.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.usenekko.home.domain.BadgeSlot
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
    val badgeSlots: List<BadgeSlot> = emptyList(),
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
                    val badges = async { contactDataSource.getBadges() }
                    val userBadges = async { contactDataSource.getUserBadges() }
                    Quintuple(
                        profile.await(),
                        contacts.await(),
                        checkIns.await(),
                        badges.await(),
                        userBadges.await(),
                    )
                }
            }

            result.fold(
                onSuccess = { (profile, contacts, checkIns, badges, userBadges) ->
                    val profileData = (profile as? Result.Success)?.data
                    val contactList = (contacts as? Result.Success)?.data.orEmpty()
                    val checkInCount = (checkIns as? Result.Success)?.data?.size ?: 0
                    val catalog = (badges as? Result.Success)?.data.orEmpty()
                    val unlockedIds = (userBadges as? Result.Success)?.data
                        .orEmpty()
                        .mapTo(mutableSetOf()) { it.badgeId }
                    val badgeSlots = catalog
                        .sortedBy { it.threshold }
                        .map { badge -> BadgeSlot(badge, badge.id in unlockedIds) }
                    _state.value = AccountState(
                        isLoading = false,
                        profile = profileData,
                        fullName = profileData?.resolvedName,
                        createdAt = profileData?.createdAt,
                        totalContacts = contactList.size,
                        totalCheckIns = checkInCount,
                        badgeSlots = badgeSlots,
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

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)