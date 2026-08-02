package app.usenekko.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.AudienceOption
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.AmbientGlow
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.GlassBottomNavBar
import app.usenekko.designsystem.navbar.top.NekkoTopBar
import app.usenekko.designsystem.shapes.SawToothCircleShape
import app.usenekko.home.addcontact.AddContactScreen
import app.usenekko.home.di.rememberAddContactViewModel
import app.usenekko.home.di.rememberHomeViewModel
import app.usenekko.home.domain.CheckIn
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.isOutstanding
import app.usenekko.home.domain.nextCheckInDateLocal
import app.usenekko.home.presentation.components.CheckInTimelineGrid
import app.usenekko.home.presentation.components.StatusSummaryCard
import app.usenekko.home.presentation.components.TimelineEvent
import app.usenekko.home.presentation.components.rememberTimelineSlots
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_acquaintance
import nekko.home.generated.resources.ic_family
import nekko.home.generated.resources.ic_fire
import nekko.home.generated.resources.ic_friends
import nekko.home.generated.resources.ic_globe
import nekko.home.generated.resources.ic_group
import nekko.home.generated.resources.ic_person
import nekko.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.DrawableResource

private fun audienceIcon(name: String): DrawableResource = when (name.lowercase()) {
    "family" -> Res.drawable.ic_family
    "friends" -> Res.drawable.ic_friends
    "acquaintance" -> Res.drawable.ic_acquaintance
    "others" -> Res.drawable.ic_person
    else -> Res.drawable.ic_person
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {

    val viewModel = rememberHomeViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAddContact by remember { mutableStateOf(false) }

    val options = remember(state.groups) {
        buildList {
            add(AudienceOption("Everyone", Res.drawable.ic_group))
            state.groups.forEach { group ->
                add(AudienceOption(group.name, audienceIcon(group.name)))
            }
        }
    }
    val selectedAudience = remember(state.selectedGroupId, options) {
        if (state.selectedGroupId == null) {
            options.first()
        } else {
            val index = state.groups.indexOfFirst { it.id == state.selectedGroupId }
            options.getOrElse(index + 1) { options.first() }
        }
    }
    val liquidState = rememberLiquidState()



    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background/source for the liquid effect
        AmbientGlow(
            liquidState = liquidState,
            modifier = Modifier.matchParentSize()
        )

        Scaffold(topBar = {
            NekkoTopBar(
                audienceOptions = options,
                selectedAudience = selectedAudience,
                onAudienceSelect = { option ->
                    val index = options.indexOf(option)
                    viewModel.onGroupSelected(
                        if (index <= 0) null else state.groups[index - 1].id
                    )
                },
                userName = "Jane Bell",
                onAvatarClick = {},
                onPremiumClick = {},
            )

        }, bottomBar = {
            GlassBottomNavBar(
                // EFFECT, sibling
                selectedIndex = 1,
                onItemSelected = {},
                onAddClick = { showAddContact = true },
                liquidState = liquidState,
            )

        }, containerColor = NekkoTheme.colors.background.b0)
        { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(40.dp))

                StatusSummaryCard(
                    outstandingCount = state.outstandingCount,
                    upToDateCount = state.upToDateCount,
                    outstandingBgResource = Res.drawable.ic_globe,
                    upToDateBgResource = Res.drawable.ic_fire,
                    gradientOrbResource = Res.drawable.img_gradientss
                )

                Spacer(Modifier.height(70.dp))

                if (state.totalContactCount == 0 && !state.isLoading) {
                    Column(
                        modifier = Modifier.clickable { showAddContact = true },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(SawToothCircleShape())
                                .background(NekkoTheme.colors.background.b2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NekkoTheme.colors.text.tertiary,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        Spacer(Modifier.height(30.dp))

                        Text(
                            "Get started",
                            color = NekkoTheme.colors.text.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Import from your contact",
                            color = NekkoTheme.colors.text.tertiary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    CheckInSection(
                        checkIns = state.checkIns,
                        contacts = state.contacts,
                        outstandingCount = state.outstandingCount,
                        checkingInContactId = state.checkingInContactId,
                        onCheckIn = viewModel::checkIn,
                    )
                }
            }

        }
    }

    if (showAddContact) {
        val addContactViewModel = rememberAddContactViewModel()
        AddContactScreen(
            viewModel = addContactViewModel,
            onDismiss = { showAddContact = false },
            onSaved = {
                showAddContact = false
                viewModel.loadContacts()
            },
        )
    }

}


@Composable
private fun CheckInSection(
    checkIns: List<CheckIn>,
    contacts: List<Contact>,
    outstandingCount: Int,
    checkingInContactId: String?,
    onCheckIn: (String) -> Unit,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val events = remember(checkIns, outstandingCount, today) {
        buildCheckInEvents(checkIns, outstandingCount, today)
    }

    Column {
        Text(
            "Check In",
            style = NekkoTheme.typography.heading2,
            color = NekkoTheme.colors.text.primary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = when {
                outstandingCount == 0 -> "You're all caught up"
                outstandingCount == 1 -> "1 contact waiting for check in"
                else -> "$outstandingCount contacts waiting for check in"
            },
            color = NekkoTheme.colors.text.tertiary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        CheckInTimelineGrid(
            slots = rememberTimelineSlots(today = today, events = events),
            animatePulse = true,
            modifier = Modifier.padding(top = 24.dp),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "Contacts",
            style = NekkoTheme.typography.heading2,
            color = NekkoTheme.colors.text.primary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        if (contacts.isEmpty()) {
            Text(
                "No contacts in this audience",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            contacts.forEach { contact ->
                ContactCheckInRow(
                    contact = contact,
                    today = today,
                    checkingInContactId = checkingInContactId,
                    onCheckIn = onCheckIn,
                )
            }
        }
    }
}

private fun buildCheckInEvents(
    checkIns: List<CheckIn>,
    outstandingCount: Int,
    today: LocalDate,
): List<TimelineEvent> {
    val byDate = checkIns
        .mapNotNull { row -> runCatching { Instant.parse(row.checkedInAt) }.getOrNull() }
        .map { instant -> instant.toLocalDateTime(TimeZone.currentSystemDefault()).date }
        .groupingBy { it }
        .eachCount()
        .map { (date, count) -> TimelineEvent(date = date, checkedIn = true, avatarCount = count) }
    return if (outstandingCount > 0) {
        byDate + TimelineEvent(date = today, checkedIn = false, avatarCount = outstandingCount)
    } else {
        byDate
    }
}

@Composable
private fun ContactCheckInRow(
    contact: Contact,
    today: LocalDate,
    checkingInContactId: String?,
    onCheckIn: (String) -> Unit,
) {
    val outstanding = contact.isOutstanding(today)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    contact.avatarColor
                        ?.let { rememberColorFromHex(it) }
                        ?: NekkoTheme.colors.fill.secondary
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                contact.name,
                color = NekkoTheme.colors.text.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                contactStatusText(contact, today),
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (outstanding) {
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { onCheckIn(contact.id) },
                enabled = checkingInContactId == null,
            ) {
                Text(
                    if (checkingInContactId == contact.id) "Checking..." else "Check In",
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun contactStatusText(contact: Contact, today: LocalDate): String {
    val next = contact.nextCheckInDateLocal()
    return when {
        next == null -> "Not scheduled"
        next <= today -> "Outstanding"
        else -> "Next: $next"
    }
}

private fun rememberColorFromHex(hex: String): Color {
    val value = hex.removePrefix("#").toLongOrNull(16) ?: return Color.Gray
    return Color((0xFF000000L or value).toInt())
}

@PreviewLightDark
@Composable
fun PreviewHomeScreen() {
    NekkoTheme {
        HomeScreen()
    }
}
