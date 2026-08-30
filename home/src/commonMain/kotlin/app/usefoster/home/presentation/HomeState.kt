package app.usefoster.home.presentation

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.Group
import app.usefoster.home.domain.MissedCheckIn
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

data class HomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val totalContactCount: Int = 0,
    val allContacts: List<Contact> = emptyList(),
    val outstandingCount: Int = 0,
    val upToDateCount: Int = 0,
    val contacts: List<Contact> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val missedCheckIns: List<MissedCheckIn> = emptyList(),
    val checkInCounts: Map<String, Int> = emptyMap(),
    val initialCountdownStartDate: LocalDate? = null,
    val checkingInContactIds: Set<String> = emptySet(),
    val checkInError: StringResource? = null,
    val error: StringResource? = null,
)
