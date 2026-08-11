package app.usenekko.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import app.usenekko.home.domain.checkInCountdownLabel
import app.usenekko.home.domain.forTodayCheckInList
import app.usenekko.home.domain.isCheckedInToday
import app.usenekko.home.domain.nextUpcomingCheckInTargetEpochMillis
import app.usenekko.home.presentation.components.CheckInTimelineGrid
import app.usenekko.home.presentation.components.CHECK_IN_BUBBLE_DURATION_MILLIS
import app.usenekko.home.presentation.components.ContactAvatar
import app.usenekko.home.presentation.components.StatusSummaryCard
import app.usenekko.home.presentation.components.buildCheckInTimelineEvents
import app.usenekko.home.presentation.components.isCheckInBubbleAnimationEnabled
import app.usenekko.home.presentation.components.rememberTimelineSlots
import app.usenekko.home.presentation.components.shouldStartCheckInBubbleWindow
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.delay
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_acquaintance
import nekko.home.generated.resources.ic_circlecheck
import nekko.home.generated.resources.ic_circlecheckmark
import nekko.home.generated.resources.ic_family
import nekko.home.generated.resources.ic_fire
import nekko.home.generated.resources.ic_friends
import nekko.home.generated.resources.ic_globe
import nekko.home.generated.resources.ic_group
import nekko.home.generated.resources.ic_person
import nekko.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.milliseconds

private fun audienceIcon(name: String): DrawableResource = when (name.lowercase()) {
    "family" -> Res.drawable.ic_family
    "friends" -> Res.drawable.ic_friends
    "acquaintance" -> Res.drawable.ic_acquaintance
    "others" -> Res.drawable.ic_person
    else -> Res.drawable.ic_person
}

@Composable
fun HomeScreen(
    onContactClick: (Contact) -> Unit,
    onSettingsClick: () -> Unit = {},
    onShowPaywall: () -> Unit = {},
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
    val blurModifier = if (showAddContact) Modifier.blur(20.dp) else Modifier



    Box(
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier),
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
                onAvatarClick = { onSettingsClick() },
                onPremiumClick = onShowPaywall,
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
                Spacer(Modifier.height(32.dp))

                StatusSummaryCard(
                    outstandingCount = state.outstandingCount,
                    upToDateCount = state.upToDateCount,
                    outstandingBgResource = Res.drawable.ic_globe,
                    upToDateBgResource = Res.drawable.ic_fire,
                    gradientOrbResource = Res.drawable.img_gradientss
                )

                Spacer(Modifier.height(32.dp))

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
                    Spacer(Modifier.height(32.dp))
                    CheckInSection(
                        checkIns = state.checkIns,
                        checkInCounts = state.checkInCounts,
                        contacts = state.contacts,
                        outstandingCount = state.outstandingCount,
                        checkingInContactId = state.checkingInContactId,
                        onCheckIn = viewModel::checkIn,
                        onContactClick = onContactClick,
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
            onShowPaywall = {
                showAddContact = false
                onShowPaywall()
            },
        )
    }

}


@Composable
private fun CheckInSection(
    checkIns: List<CheckIn>,
    checkInCounts: Map<String, Int>,
    contacts: List<Contact>,
    outstandingCount: Int,
    checkingInContactId: String?,
    onCheckIn: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var nowEpochMillis by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowEpochMillis = Clock.System.now().toEpochMilliseconds()
            delay(1_000L.milliseconds)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var bubbleWindowActive by remember(lifecycleOwner) { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appInForeground = true
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY,
                    -> {
                    appInForeground = false
                    bubbleWindowActive = false
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val events = remember(checkIns, contacts, today) {
        buildCheckInTimelineEvents(checkIns, contacts, today)
    }
    val slots = rememberTimelineSlots(today = today, events = events)
    val hasPendingToday = slots.any { it.isCurrent && it.hasPendingCheckIn }
    LaunchedEffect(appInForeground, hasPendingToday) {
        bubbleWindowActive = shouldStartCheckInBubbleWindow(appInForeground, hasPendingToday)
    }

    Column {
        Text(
            "Check In",
            style = NekkoTheme.typography.heading2,
            color = NekkoTheme.colors.text.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 1.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (outstandingCount) {
                0 -> "No check-in today"
                1 -> "1 contact waiting for check in"
                else -> "$outstandingCount contacts waiting for check in"
            },
            color = NekkoTheme.colors.text.tertiary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 1.dp)

        )
        Spacer(Modifier.height(4.dp))
        CheckInTimelineGrid(
            slots = slots,
            animateBubble = isCheckInBubbleAnimationEnabled(
                appInForeground = appInForeground,
                hasPendingToday = hasPendingToday,
                bubbleWindowActive = bubbleWindowActive,
            ),
            modifier = Modifier.padding(top = 24.dp),
        )
        Spacer(Modifier.height(32.dp))
        if (contacts.isEmpty()) {
            Text(
                "No contacts in this audience",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            val todayContacts = contacts.forTodayCheckInList(today)
            val nextUpcomingTarget = contacts.nextUpcomingCheckInTargetEpochMillis(nowEpochMillis)
            if (todayContacts.isEmpty()) {
                NextCheckInCard(
                    targetEpochMillis = nextUpcomingTarget,
                    nowEpochMillis = nowEpochMillis,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(NekkoTheme.colors.fill.quaternary)
                        .padding(horizontal = 16.dp),
                ) {
                    todayContacts.forEachIndexed { index, contact ->
                        ContactCheckInRow(
                            contact = contact,
                            checkInCount = checkInCounts[contact.id] ?: 0,
                            today = today,
                            checkingInContactId = checkingInContactId,
                            onCheckIn = onCheckIn,
                            onContactClick = onContactClick,
                            showDivider = index < todayContacts.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCheckInRow(
    contact: Contact,
    checkInCount: Int,
    today: LocalDate,
    checkingInContactId: String?,
    onCheckIn: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    showDivider: Boolean,
) {
    val checkedInToday = contact.isCheckedInToday(today)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onContactClick(contact) }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactAvatar(
                avatarColor = contact.avatarColor,
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, NekkoTheme.colors.stroke.secondary, CircleShape),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    contact.name,
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    checkInCountLabel(checkInCount),
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (!checkedInToday) {
                Button(
                    onClick = { onCheckIn(contact.id) },
                    enabled = checkingInContactId == null,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = NekkoTheme.colors.text.primary,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Color.Black,
                    ),
                ) {
                    Text(
                        if (checkingInContactId == contact.id) "Checking..." else "Check in",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            } else {

                Icon(
                    imageVector = vectorResource(Res.drawable.ic_circlecheckmark),
                    contentDescription = "Checked in",
                    modifier = Modifier.size(24.dp),
                    tint = NekkoTheme.colors.gray.secondary,
                )

            }
        }
        if (showDivider) DashedContactDivider()
    }
}

@Composable
private fun NextCheckInCard(
    targetEpochMillis: Long?,
    nowEpochMillis: Long,
) {
    val label = targetEpochMillis
        ?.let { checkInCountdownLabel(it - nowEpochMillis) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.background.b1)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label?.let { "Next check-in in $it" } ?: "No upcoming check-ins",
            color = NekkoTheme.colors.text.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun checkInCountLabel(count: Int): String = when (count) {
    1 -> "1 check-in"
    else -> "$count check-ins"
}

@Composable
private fun DashedContactDivider() {
    val dividerColor = NekkoTheme.colors.stroke.secondary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = dividerColor,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
        )
    }
}

@PreviewLightDark
@Composable
fun PreviewHomeScreen() {
    NekkoTheme {
        HomeScreen(onContactClick = {})
    }
}
