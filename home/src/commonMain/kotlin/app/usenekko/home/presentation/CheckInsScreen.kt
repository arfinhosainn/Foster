package app.usenekko.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.adaptive.WindowWidthSizeClass
import app.usenekko.adaptive.windowWidthSizeClass
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.AmbientGlow
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.GlassBottomNavBar
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.GlassNavigationRail
import app.usenekko.designsystem.navbar.top.NekkoTopBar
import app.usenekko.designsystem.shapes.SawToothCircleShape
import app.usenekko.home.di.LocalAccountRepository
import app.usenekko.home.di.rememberAddContactViewModel
import app.usenekko.home.di.rememberGroupSettingsViewModel
import app.usenekko.home.addcontact.AddContactScreen
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.Group
import app.usenekko.home.domain.GroupMembership
import app.usenekko.home.presentation.components.ContactAvatar
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState

private const val PhoneGroupColumns = 2

fun resolveCheckInsAddClick(
    onAddClick: (() -> Unit)?,
    onOpenAddContact: () -> Unit,
): () -> Unit = onAddClick ?: onOpenAddContact

@Composable
fun CheckInsScreen(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onShowPaywall: () -> Unit = {},
    onAddClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberGroupSettingsViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accountRepository = LocalAccountRepository.current
    val accountState by accountRepository.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val liquidState = rememberLiquidState()
    var showAddContact by rememberSaveable { mutableStateOf(false) }
    val handleAddClick = resolveCheckInsAddClick(onAddClick) { showAddContact = true }

    LaunchedEffect(accountRepository) {
        accountRepository.load()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refreshIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = windowWidthSizeClass(maxWidth) == WindowWidthSizeClass.Expanded

        AmbientGlow(liquidState, Modifier.matchParentSize())

        if (useNavigationRail) {
            GlassNavigationRail(
                selectedIndex = 1,
                onItemSelected = { index -> if (index == 0) onHomeClick() },
                onAddClick = handleAddClick,
                liquidState = liquidState,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useNavigationRail) Modifier.padding(start = 88.dp) else Modifier),
            topBar = {
                NekkoTopBar(
                    title = "Check-ins",
                    userName = accountState.snapshot?.profile?.resolvedName.orEmpty(),
                    avatarContent = {
                        ContactAvatar(
                            avatarColor = null,
                            selectedAvatarId = accountState.snapshot?.profile?.selectedAvatarId,
                            modifier = Modifier.size(44.dp),
                        )
                    },
                    onAvatarClick = onSettingsClick,
                    onPremiumClick = onShowPaywall,
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    GlassBottomNavBar(
                        selectedIndex = 1,
                        onItemSelected = { index -> if (index == 0) onHomeClick() },
                        onAddClick = handleAddClick,
                        liquidState = liquidState,
                    )
                }
            },
            containerColor = NekkoTheme.colors.background.b0,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        color = NekkoTheme.colors.text.tertiary,
                    )

                    state.groups.isEmpty() -> EmptyGroupsState()

                    else -> GroupGrid(
                        groups = state.groups,
                        contacts = state.contacts,
                        memberships = state.memberships,
                    )
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = Color(0xFFFF4B4B),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
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
                viewModel.load(forceRefresh = true)
            },
            onShowPaywall = {
                showAddContact = false
                onShowPaywall()
            },
        )
    }
}

@Composable
private fun EmptyGroupsState() {
    Spacer(Modifier.height(32.dp))
    Text(
        text = "No groups yet",
        color = NekkoTheme.colors.text.primary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Create a group from Settings to organize your check-ins",
        color = NekkoTheme.colors.text.tertiary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GroupGrid(
    groups: List<Group>,
    contacts: List<Contact>,
    memberships: List<GroupMembership>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = (maxWidth / 180.dp).toInt().coerceIn(PhoneGroupColumns, 4)

        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            groups.chunked(columns).forEach { rowGroups ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowGroups.forEach { group ->
                        val members = groupMembers(group.id, contacts, memberships)
                        GroupCell(
                            group = group,
                            members = members,
                            memberCount = memberships.count { it.groupId == group.id },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowGroups.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun groupMembers(
    groupId: String,
    contacts: List<Contact>,
    memberships: List<GroupMembership>,
): List<Contact> {
    val memberIds = memberships
        .asSequence()
        .filter { it.groupId == groupId }
        .map { it.contactId }
        .toSet()
    return contacts.filter { it.id in memberIds }
}

@Composable
private fun GroupCell(
    group: Group,
    members: List<Contact>,
    memberCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GroupCard(
            members = members,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = group.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "$memberCount ${if (memberCount == 1) "person" else "people"}",
            style = NekkoTheme.typography.footnote,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GroupCard(
    members: List<Contact>,
    modifier: Modifier = Modifier,
) {
    val avatarRingBrush = remember {
        Brush.sweepGradient(
            listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
        )
    }

    Box(
        modifier = modifier
            .clip(SawToothCircleShape())
            .background(NekkoTheme.colors.fill.secondary),
        contentAlignment = Alignment.Center,
    ) {
        if (members.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "No members",
                tint = NekkoTheme.colors.text.quaternary,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Row {
                members.take(3).forEachIndexed { index, contact ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .then(
                                if (index > 0) Modifier.offset(x = -(18.dp * index))
                                else Modifier,
                            )
                            .clip(CircleShape)
                            .border(1.5.dp, avatarRingBrush, CircleShape)
                            .background(NekkoTheme.colors.fill.secondary)
                            .padding(2.dp),
                    ) {
                        ContactAvatar(
                            avatarColor = contact.avatarColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (members.size > 3) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .offset(x = -(18.dp * 3))
                            .clip(CircleShape)
                            .background(NekkoTheme.colors.fill.tertiary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+${members.size - 3}",
                            color = NekkoTheme.colors.text.secondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}