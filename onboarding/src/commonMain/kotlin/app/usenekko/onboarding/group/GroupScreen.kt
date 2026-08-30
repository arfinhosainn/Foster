package app.usenekko.onboarding.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.shapes.SawToothCircleShape
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.designsystem.avatar.avatarResources
import app.usenekko.onboarding.group.components.CreateGroupBottomSheet
import app.usenekko.onboarding.presentation.LocalOnboardingDraftStore
import app.usenekko.onboarding.presentation.rememberGroupViewModel
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource
import nekko.onboarding.generated.resources.action_next
import nekko.onboarding.generated.resources.add_step_group_title
import nekko.onboarding.generated.resources.add_tap_plus_button
import nekko.onboarding.generated.resources.add_wanna_create_group
import nekko.onboarding.generated.resources.cd_add_members
import nekko.onboarding.generated.resources.cd_group_member_avatar
import nekko.onboarding.generated.resources.group_add_user_subtitle
import org.jetbrains.compose.resources.stringResource

private const val GroupColumns = 2
private val StarterIds = setOf("family", "friends")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberGroupViewModel()
    val showCreateGroupSheet by viewModel.showCreateGroupSheet.collectAsStateWithLifecycle()

    val draftStore = LocalOnboardingDraftStore.current
    val draft by draftStore.draft.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                GroupEvent.NavigateToNext -> onNavigateToNext()
                GroupEvent.NavigateBack -> onBack()
            }
        }
    }

    val starterGroups = remember {
        listOf(
            Group(
                id = "family",
                name = "Family",
            ),
            Group(
                id = "friends",
                name = "Friends",
            ),
        )
    }

    // Every created group joins the same grid as the starters.
    val allGroups = remember(draft.groups, starterGroups) {
        starterGroups + draft.groups
            .filter { it.id !in StarterIds }
            .map { Group(id = it.id, name = it.name) }
    }

    val liquidState = rememberLiquidState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NekkoTheme.colors.background.b0)
                .liquefiable(liquidState),
        )

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        scrolledContainerColor = NekkoTheme.colors.background.b0,
                    ),
                    title = { StepIndicator(totalSteps = 7, currentStep = 1) },
                    navigationIcon = { },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                AdaptiveSurface {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NekkoTheme.colors.background.b0)
                            .padding(bottom = 24.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NekkoActionButton(
                            onClick = { viewModel.onBackClicked() },
                            leadingIcon = vectorResource(Res.drawable.ic_back),
                            modifier = Modifier.weight(0.19f),
                        )

                        Spacer(Modifier.width(12.dp))

                        NekkoButton(
                            text = stringResource(Res.string.action_next),
                            onClick = { viewModel.onNextClicked() },
                            modifier = Modifier.weight(0.8f),
                        )
                    }
                }
            },
            containerColor = NekkoTheme.colors.background.b0,
        ) { innerPadding ->
            AdaptiveSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                Spacer(Modifier.height(40.dp))

                Text(
                    text = stringResource(Res.string.add_step_group_title),
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NekkoTheme.colors.text.primary,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.group_add_user_subtitle),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary,
                )

                Spacer(Modifier.height(42.dp))

                GroupGrid(
                    groups = allGroups,
                    selectedGroupId = draft.selectedGroupId,
                    selectedAvatarId = draft.selectedAvatarId,
                    onGroupClick = viewModel::onGroupSelected,
                )

                Spacer(Modifier.height(40.dp))

                NekkoActionButton(
                    onClick = { viewModel.onCreateGroupClicked() },
                    leadingIcon = vectorResource(Res.drawable.ic_add),
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.add_wanna_create_group),
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.add_tap_plus_button),
                    color = NekkoTheme.colors.text.tertiary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))
            }
            }
        }
    }

    if (showCreateGroupSheet) {
        CreateGroupBottomSheet(
            onDismiss = { viewModel.onDismissCreateGroupSheet() },
            onSave = { name -> viewModel.onSaveGroup(name) },
        )
    }
}

@Composable
private fun GroupGrid(
    groups: List<Group>,
    selectedGroupId: String?,
    selectedAvatarId: String?,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 25.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
) {
    groups.chunked(GroupColumns).forEach { rowGroups ->
        val isShortRow = rowGroups.size < GroupColumns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(27.dp),
        ) {
            // Half a cell of padding on each side centers a lone trailing item.
            if (isShortRow) Spacer(Modifier.weight(0.5f))

            rowGroups.forEach { group ->
                GroupCell(
                    group = group,
                    memberAvatarIds = groupAvatarIds(
                        groupId = group.id,
                        selectedGroupId = selectedGroupId,
                        selectedAvatarId = selectedAvatarId,
                    ),
                    onClick = { onGroupClick(group.id) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (isShortRow) Spacer(Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun GroupCell(
    group: Group,
    memberAvatarIds: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    GroupCard(
        group = group,
        memberAvatarIds = memberAvatarIds,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
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
        text = "${memberAvatarIds.size} ${if (memberAvatarIds.size == 1) "person" else "people"}",
        style = NekkoTheme.typography.footnote,
        fontWeight = FontWeight.Medium,
        color = NekkoTheme.colors.text.tertiary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GroupCard(
    group: Group,
    memberAvatarIds: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(SawToothCircleShape())
            .background(NekkoTheme.colors.fill.secondary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (memberAvatarIds.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.cd_add_members),
                tint = NekkoTheme.colors.text.quaternary,
                modifier = Modifier.size(40.dp),
            )
        } else {
            MemberAvatars(avatarIds = memberAvatarIds)
        }
    }
}

@Composable
private fun MemberAvatars(
    avatarIds: List<String>,
    modifier: Modifier = Modifier,
) {
    val avatarSize = 56.dp
    val overlapOffset = 18.dp

    val avatarRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row {
            avatarIds.take(3).forEachIndexed { index, avatarId ->
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .then(
                            if (index > 0) Modifier.offset(x = -(overlapOffset * index))
                            else Modifier,
                        )
                        .clip(CircleShape)
                        .border(1.5.dp, avatarRingBrush, CircleShape)
                        .background(NekkoTheme.colors.fill.secondary),
                ) {
                    avatarResources.getOrNull(avatarId.toIntOrNull() ?: -1)?.let { resource ->
                        Image(
                            imageVector = vectorResource(resource),
                            contentDescription = stringResource(Res.string.cd_group_member_avatar),
                            modifier = Modifier
                                .size(avatarSize - 4.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape),
                        )
                    }
                }
            }

            if (avatarIds.size > 3) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .offset(x = -(overlapOffset * 3))
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.fill.tertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+${avatarIds.size - 3}",
                        color = NekkoTheme.colors.text.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

internal fun groupAvatarIds(
    groupId: String,
    selectedGroupId: String?,
    selectedAvatarId: String?,
): List<String> = if (groupId == selectedGroupId) {
    selectedAvatarId?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
} else {
    emptyList()
}

@PreviewLightDark
@Composable
private fun GroupScreenPreview() {
    NekkoTheme {
        GroupScreen(
            onNavigateToNext = {},
            onBack = {},
        )
    }
}