package app.usefoster.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.usefoster.designsystem.buttons.AudienceOption
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import app.usefoster.navigation.BottomBarActions
import app.usefoster.designsystem.navbar.top.FosterTopBar
import app.usefoster.adaptive.WindowWidthSizeClass
import app.usefoster.adaptive.AdaptivePresentation
import app.usefoster.adaptive.contactPresentation
import app.usefoster.adaptive.retainPaneSelection
import app.usefoster.adaptive.windowWidthSizeClass
import app.usefoster.designsystem.shapes.SawToothCircleShape
import app.usefoster.designsystem.sideShine
import app.usefoster.home.addcontact.AddContactScreen
import app.usefoster.home.di.rememberAddContactViewModel
import app.usefoster.home.di.rememberHomeViewModel
import app.usefoster.home.di.LocalAccountRepository
import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.checkInCountdownLabel
import app.usefoster.home.domain.forTodayCheckInList
import app.usefoster.home.domain.isCheckedInToday
import app.usefoster.home.domain.nextUpcomingCheckInTargetEpochMillis
import app.usefoster.home.presentation.components.CheckInTimelineGrid
import app.usefoster.home.presentation.components.CHECK_IN_BUBBLE_DURATION_MILLIS
import app.usefoster.home.presentation.components.ContactAvatar
import app.usefoster.home.presentation.components.StatusSummaryCard
import app.usefoster.home.presentation.components.buildCheckInTimelineEvents
import app.usefoster.home.presentation.components.isCheckInBubbleAnimationEnabled
import app.usefoster.home.presentation.components.rememberTimelineSlots
import app.usefoster.home.presentation.components.shouldStartCheckInBubbleWindow
import app.usefoster.shared.notifications.HomeCheckInListSignal
import app.usefoster.home.presentation.components.timelineMaxCellSizeForWidth
import app.usefoster.home.presentation.components.updateTimelineDate
import app.usefoster.home.presentation.contactprofile.ContactProfileScreen
import app.usefoster.home.presentation.HomeLoadingSkeleton
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.theme.FosterTheme
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import foster.home.generated.resources.Res
import foster.home.generated.resources.home_load_error_title
import foster.home.generated.resources.ic_acquaintance
import foster.home.generated.resources.ic_circlecheck
import foster.home.generated.resources.ic_circlecheckmark
import foster.home.generated.resources.ic_family
import foster.home.generated.resources.ic_fire
import foster.home.generated.resources.ic_friends
import foster.home.generated.resources.ic_globe
import foster.home.generated.resources.ic_group
import foster.home.generated.resources.ic_person
import foster.home.generated.resources.img_gradientss
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import foster.home.generated.resources.action_try_again
import foster.home.generated.resources.audience_everyone
import foster.home.generated.resources.cd_checked_in
import foster.home.generated.resources.home_checkin_error
import foster.home.generated.resources.home_contacts_waiting_many
import foster.home.generated.resources.home_contacts_waiting_one
import foster.home.generated.resources.home_next_checkin_in
import foster.home.generated.resources.home_no_checkin_today
import foster.home.generated.resources.home_no_upcoming_checkins
import foster.home.generated.resources.home_select_contact
import foster.home.generated.resources.home_select_contact_hint
import foster.home.generated.resources.home_updating
import org.jetbrains.compose.resources.stringResource

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
    onBrainstormClick: (String) -> Unit = {},
    onCheckInsClick: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onShowPaywall: () -> Unit = {},
    onShowDiscountPaywall: () -> Unit = {},
    bottomBarActions: BottomBarActions? = null,
    modifier: Modifier = Modifier,
) {

    val viewModel = rememberHomeViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accountRepository = LocalAccountRepository.current
    val accountState by accountRepository.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Digest-notification taps land on Home scrolled to the check-in contact
    // list. The signal is a monotonic trigger id; the section's Y offset is
    // measured via onGloballyPositioned at the CheckInSection call site.
    val homeScrollState = rememberScrollState()
    val checkInListSignal by HomeCheckInListSignal.pending.collectAsState()
    var checkInSectionTop by remember { mutableStateOf(0f) }
    LaunchedEffect(checkInListSignal) {
        if (checkInListSignal > 0) {
            // Cold start: wait until the section has been laid out.
            while (checkInSectionTop <= 0f) delay(50)
            homeScrollState.animateScrollTo(checkInSectionTop.toInt())
        }
    }

    LaunchedEffect(accountRepository) {
        accountRepository.load()
    }

    LaunchedEffect(accountState.snapshot?.accountKey) {
        if (accountState.snapshot != null) {
            viewModel.loadContacts(forceRefresh = true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.refreshIfStale()
                coroutineScope.launch { accountRepository.load() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showAddContact by rememberSaveable { mutableStateOf(false) }

    // React to the shell-owned persistent bottom bar. Delivered as an event
    // stream instead of a stored callback: during transitions both tabs are
    // briefly composed, and a dispose-time unregister from the outgoing screen
    // could otherwise leave the bar's "+" dead. Rare double-fire is possible if
    // "+" is tapped mid-transition; harmless (both sheets converge).
    val actions = bottomBarActions
    if (actions != null) {
        LaunchedEffect(actions) {
            snapshotFlow { actions.addContactRequestCount }
                .drop(1)
                .collect { showAddContact = true }
        }
        LaunchedEffect(actions) {
            snapshotFlow { showAddContact }
                .collect { actions.isOverlayShowing = it }
        }
    }

    val everyoneOption = stringResource(Res.string.audience_everyone)
    val options = remember(state.groups, everyoneOption) {
        buildList {
            add(AudienceOption(everyoneOption, Res.drawable.ic_group))
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
    val blurModifier = if (showAddContact) Modifier.blur(20.dp) else Modifier



    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier),
    ) {
        val widthSizeClass = windowWidthSizeClass(maxWidth)
        val useNavigationRail = widthSizeClass == WindowWidthSizeClass.Expanded
        val useSupportingPane = contactPresentation(widthSizeClass) == AdaptivePresentation.SupportingPane
        val timelineMaxCellSize = timelineMaxCellSizeForWidth(maxWidth)

        var selectedContactId by rememberSaveable { mutableStateOf<String?>(null) }
        val selectedContact = state.contacts.firstOrNull { it.id == selectedContactId }

        LaunchedEffect(state.contacts) {
            selectedContactId = retainPaneSelection(
                selectedId = selectedContactId,
                availableIds = state.contacts.map(Contact::id),
            )
        }

        // Background/source for the liquid effect


        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
            FosterTopBar(
                audienceOptions = options,
                selectedAudience = selectedAudience,
                onAudienceSelect = { option ->
                    val index = options.indexOf(option)
                    viewModel.onGroupSelected(
                        if (index <= 0) null else state.groups[index - 1].id
                    )
                },
                userName = accountState.snapshot?.profile?.resolvedName.orEmpty(),
                avatarContent = {
                    ContactAvatar(
                        avatarColor = null,
                        selectedAvatarId = accountState.snapshot?.profile?.selectedAvatarId,
                        modifier = Modifier.size(44.dp),
                    )
                },
                onAvatarClick = { onSettingsClick() },
                onPremiumClick = onShowPaywall,
            )
            },
            containerColor = FosterTheme.colors.background.b0,
        )
        { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalArrangement = if (useSupportingPane) {
                    Arrangement.spacedBy(16.dp)
                } else {
                    Arrangement.Center
                },
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(homeScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                Spacer(Modifier.height(32.dp))

                if (state.isLoading) {
                    HomeLoadingSkeleton(timelineMaxCellSize = timelineMaxCellSize)
                } else {
                    StatusSummaryCard(
                        outstandingCount = state.outstandingCount,
                        upToDateCount = state.upToDateCount,
                        outstandingBgResource = Res.drawable.ic_globe,
                        upToDateBgResource = Res.drawable.ic_fire,
                        gradientOrbResource = Res.drawable.img_gradientss,
                        onOutstandingClick = onOpenHistory,
                        onUpToDateClick = onOpenHistory,
                    )

                    state.checkInError?.let {
                        Text(
                            text = stringResource(Res.string.home_checkin_error),
                            color = FosterTheme.colors.text.tertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    }

                    if (state.isRefreshing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = FosterTheme.colors.text.tertiary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.home_updating),
                                color = FosterTheme.colors.text.tertiary,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    if (state.totalContactCount == 0 && state.error != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                stringResource(Res.string.home_load_error_title),
                                color = FosterTheme.colors.text.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(state.error!!),
                                color = FosterTheme.colors.text.tertiary,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadContacts(forceRefresh = true) }) {
                                Text(stringResource(Res.string.action_try_again))
                            }
                        }
                    } else if (state.totalContactCount == 0) {
                        Column(
                            modifier = Modifier.clickable(role = Role.Button) { showAddContact = true },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(SawToothCircleShape())
                                    .background(FosterTheme.colors.background.b2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = FosterTheme.colors.text.tertiary,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            Spacer(Modifier.height(30.dp))

                            Text(
                                "Get started",
                                color = FosterTheme.colors.text.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(10.dp))

                            Text(
                                "Import from your contact",
                                color = FosterTheme.colors.text.tertiary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(Modifier.height(32.dp))
                        CheckInSection(
                            checkIns = state.checkIns,
                            missedCheckIns = state.missedCheckIns,
                            checkInCounts = state.checkInCounts,
                            contacts = state.contacts,
                            timelineContacts = state.allContacts,
                            outstandingCount = state.outstandingCount,
                            checkingInContactIds = state.checkingInContactIds,
                            onCheckIn = viewModel::checkIn,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                checkInSectionTop = coordinates.positionInParent().y
                            },
                            onContactClick = { contact ->
                                if (useSupportingPane) {
                                    selectedContactId = contact.id
                                } else {
                                    onContactClick(contact)
                                }
                            },
                            timelineMaxCellSize = timelineMaxCellSize,
                            initialCountdownStartDate = state.initialCountdownStartDate,
                        )

                        // Clearance so the shell-owned floating bottom bar never
                        // covers the tail of the scrollable content.
                        if (!useNavigationRail) {
                            Spacer(Modifier.navigationBarsPadding().height(104.dp))
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
                    }
                }

                if (useSupportingPane) {
                    Box(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                            .padding(end = 16.dp, bottom = 16.dp)
                            .background(
                                color = FosterTheme.colors.background.b1,
                                shape = RoundedCornerShape(28.dp),
                            ),
                    ) {
                        if (selectedContact != null) {
                            ContactProfileScreen(
                                contactId = selectedContact.id,
                                onBack = { selectedContactId = null },
                                onBrainstormClick = { onBrainstormClick(selectedContact.id) },
                                isSupportingPane = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            ContactPaneEmptyState(modifier = Modifier.fillMaxSize())
                        }
                    }
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
                addContactViewModel.resetDraft()
                viewModel.loadContacts(forceRefresh = true)
            },
            onShowPaywall = {
                showAddContact = false
                onShowPaywall()
            },
            onShowDiscountPaywall = {
                showAddContact = false
                onShowDiscountPaywall()
            },
        )
    }

}


@Composable
private fun ContactPaneEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.home_select_contact),
                color = FosterTheme.colors.text.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_select_contact_hint),
                color = FosterTheme.colors.text.tertiary,
                fontSize = 14.sp,
            )
        }
    }
}


@Composable
private fun CheckInSection(
    checkIns: List<CheckIn>,
    missedCheckIns: List<MissedCheckIn>,
    checkInCounts: Map<String, Int>,
    contacts: List<Contact>,
    timelineContacts: List<Contact>,
    outstandingCount: Int,
    checkingInContactIds: Set<String>,
    onCheckIn: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    timelineMaxCellSize: Dp,
    initialCountdownStartDate: LocalDate?,
    modifier: Modifier = Modifier,
) {
    var today by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    var nowEpochMillis by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Clock.System.now()
            nowEpochMillis = now.toEpochMilliseconds()
            today = updateTimelineDate(
                previousDate = today,
                currentDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            )
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
    val events = remember(checkIns, missedCheckIns, timelineContacts, today, initialCountdownStartDate) {
        buildCheckInTimelineEvents(
            checkIns,
            timelineContacts,
            today,
            missedCheckIns,
            initialCountdownStartDate,
        )
    }
    val slots = rememberTimelineSlots(
        today = today,
        events = events,
        initialCountdownStartDate = initialCountdownStartDate,
    )
    val hasPendingToday = slots.any { it.isCurrent && it.hasPendingCheckIn }
    LaunchedEffect(appInForeground, hasPendingToday) {
        bubbleWindowActive = shouldStartCheckInBubbleWindow(appInForeground, hasPendingToday)
    }

    Column(modifier = modifier) {
        Text(
            "Check In",
            style = FosterTheme.typography.heading2,
            color = FosterTheme.colors.text.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 1.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (outstandingCount) {
                0 -> stringResource(Res.string.home_no_checkin_today)
                1 -> stringResource(Res.string.home_contacts_waiting_one)
                else -> stringResource(Res.string.home_contacts_waiting_many, outstandingCount)
            },
            color = FosterTheme.colors.text.tertiary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 1.dp)

        )
        Spacer(Modifier.height(4.dp))
        CheckInTimelineGrid(
            slots = slots,
            maxCellSize = timelineMaxCellSize,
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
                color = FosterTheme.colors.text.tertiary,
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
                val contactListShape = RoundedCornerShape(32.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(contactListShape)
                        .background(FosterTheme.colors.fill.quaternary)
                        .sideShine(contactListShape, intensity = 0.8f)
                        .padding(horizontal = 16.dp),
                ) {
                    todayContacts.forEachIndexed { index, contact ->
                        key(contact.id) {
                            ContactCheckInRow(
                                contact = contact,
                                checkInCount = checkInCounts[contact.id] ?: 0,
                                today = today,
                                checkingInContactIds = checkingInContactIds,
                                onCheckIn = onCheckIn,
                                onContactClick = onContactClick,
                                showDivider = index < todayContacts.lastIndex,
                                index = index,
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class CheckInRowTrailing { Button, Loading, Checked }

private val CONTACT_CHECK_IN_ROW_PITCH = 89.dp
private const val CONTACT_CHECK_IN_ROW_STAGGER_MS = 100L

@Composable
private fun ContactCheckInRow(
    contact: Contact,
    checkInCount: Int,
    today: LocalDate,
    checkingInContactIds: Set<String>,
    onCheckIn: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    showDivider: Boolean,
    index: Int,
) {
    val checkedInToday = contact.isCheckedInToday(today)
    val inFlight = contact.id in checkingInContactIds
    val density = LocalDensity.current
    val rowOffset = remember { Animatable(0f) }
    var previousIndex by remember { mutableStateOf(index) }

    LaunchedEffect(index) {
        if (previousIndex == index) return@LaunchedEffect

        val oldIndex = previousIndex
        previousIndex = index
        rowOffset.snapTo(
            with(density) { (oldIndex - index) * CONTACT_CHECK_IN_ROW_PITCH.toPx() },
        )
        delay(abs(index - oldIndex) * CONTACT_CHECK_IN_ROW_STAGGER_MS)
        rowOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    val avatarRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
    )

    Column {
        Row(
            modifier = Modifier
                .offset { IntOffset(0, rowOffset.value.roundToInt()) }
                .fillMaxWidth()
                .clickable(role = Role.Button) { onContactClick(contact) }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactAvatar(
                avatarColor = contact.avatarColor,
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, avatarRingBrush, CircleShape),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    contact.name,
                    color = FosterTheme.colors.text.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    checkInCountLabel(checkInCount),
                    color = FosterTheme.colors.text.tertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = when {
                    inFlight -> CheckInRowTrailing.Loading
                    checkedInToday -> CheckInRowTrailing.Checked
                    else -> CheckInRowTrailing.Button
                },
                transitionSpec = {
                    (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "checkInState",
            ) { state ->
                when (state) {
                    CheckInRowTrailing.Button -> Button(
                        onClick = { onCheckIn(contact.id) },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = FosterTheme.colors.text.primary,
                        ),
                    ) {
                        Text(
                            "Check in",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black,
                        )
                    }
                    CheckInRowTrailing.Loading -> Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = FosterTheme.colors.text.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                    else -> Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_circlecheckmark),
                            contentDescription = stringResource(Res.string.cd_checked_in),
                            modifier = Modifier.size(24.dp),
                            tint = FosterTheme.colors.gray.secondary,
                        )
                    }
                }
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
            .background(FosterTheme.colors.background.b1)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label?.let { stringResource(Res.string.home_next_checkin_in, it) } ?: stringResource(Res.string.home_no_upcoming_checkins),
            color = FosterTheme.colors.text.primary,
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
    val dividerColor = FosterTheme.colors.stroke.secondary
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

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
@PreviewLightDark
@Composable
fun PreviewHomeScreen() {
    FosterTheme {
        HomeScreen(onContactClick = {})
    }
}
