package app.usenekko.onboarding.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoActionButton
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.shapes.SawToothCircleShape
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.domain.GroupDraft
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.group.components.CreateGroupBottomSheet
import app.usenekko.onboarding.presentation.LocalOnboardingDraftStore
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.random.Random
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

/**
 * One-time onboarding flow:
 * - show Family + Friends
 * - allow creating only one extra group
 * - after creation, hide the plus button
 * - show the created group centered together with the starter row, using a
 *   fixed gap instead of a bottom-pinned unbounded spacer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draftStore = LocalOnboardingDraftStore.current
    val draft by draftStore.draft.collectAsStateWithLifecycle()
    val starterGroups = remember {
        listOf(
            Group(
                id = "family",
                name = "Family",
                members = listOf(Color(0xFF4CAF50), Color(0xFFFF9800)),
            ),
            Group(
                id = "friends",
                name = "Friends",
                members = emptyList(),
            ),
        )
    }

    val createdGroup = draft.groups
        .firstOrNull { it.id !in setOf("family", "friends") }
        ?.toUiGroup()
    var showCreateGroupSheet by remember { mutableStateOf(false) }

    val liquidState = rememberLiquidState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NekkoTheme.colors.background.b0)
                .liquefiable(liquidState)
        )

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        StepIndicator(
                            totalSteps = 8,
                            currentStep = 2,
                        )
                    },
                    navigationIcon = { },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledIconButton(
                        modifier = modifier.weight(0.23f).size(58.dp),
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = NekkoTheme.colors.fill.tertiary)
                    ) {
                        Image(
                            imageVector = vectorResource(Res.drawable.ic_back),
                            contentDescription = "BACK"
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    NekkoButton(
                        text = "Next",
                        onClick = {
                            draftStore.update { it.copy(currentStep = OnboardingStep.DayReminder) }
                            onNavigateToNext()
                        },
                        modifier = Modifier.weight(0.8f),
                    )
                }
            },
            containerColor = NekkoTheme.colors.background.b0
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(42.dp))

                    Text(
                        text = "Add to a group",
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NekkoTheme.colors.text.primary,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Add user to a group",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.tertiary,
                    )
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            starterGroups.forEach { group ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    GroupCard(
                                        group = group,
                                        onClick = { /* optional selection */ },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f),
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        text = group.name,
                                        style = NekkoTheme.typography.heading4Semibold,
                                        color = NekkoTheme.colors.text.primary,
                                        textAlign = TextAlign.Center,
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = "${group.members.size} people",
                                        style = NekkoTheme.typography.footnote,
                                        color = NekkoTheme.colors.text.tertiary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(100.dp)) // fixed gap, not weighted

                        if (createdGroup == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                NekkoActionButton(onClick = {
                                    showCreateGroupSheet = true
                                }, leadingIcon = vectorResource(Res.drawable.ic_add))

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = "Wanna create a new group?",
                                    color = NekkoTheme.colors.text.secondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Tap the plus button",
                                    color = NekkoTheme.colors.text.tertiary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            val group = createdGroup
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GroupCard(
                                    group = group,
                                    onClick = { },
                                    modifier = Modifier.size(160.dp),
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = group.name,
                                    style = NekkoTheme.typography.heading4Semibold,
                                    color = NekkoTheme.colors.text.primary,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "${group.members.size} people",
                                    style = NekkoTheme.typography.footnote,
                                    color = NekkoTheme.colors.text.tertiary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupSheet && createdGroup == null) {
        CreateGroupBottomSheet(
            onDismiss = { showCreateGroupSheet = false },
            onSave = { name ->
                val group = GroupDraft(
                    id = "group_${Random.nextLong()}",
                    name = name,
                )
                draftStore.update {
                    it.copy(
                        groups = listOf(group),
                        currentStep = OnboardingStep.Group,
                    )
                }
                showCreateGroupSheet = false
            },
        )
    }
}

private fun GroupDraft.toUiGroup(): Group = Group(
    id = id,
    name = name,
)

@Composable
private fun GroupCard(
    group: Group,
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
        if (group.members.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add members",
                tint = NekkoTheme.colors.text.quaternary,
                modifier = Modifier.size(40.dp),
            )
        } else {
            MemberAvatars(colors = group.members)
        }
    }
}

@Composable
private fun MemberAvatars(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val avatarSize = 56.dp
    val overlapOffset = 18.dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row {
            colors.take(3).forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .then(
                            if (index > 0) Modifier.offset(x = -(overlapOffset * index))
                            else Modifier
                        )
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.background.b2),
                ) {
                    Box(
                        modifier = Modifier
                            .size(avatarSize - 4.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
            }

            if (colors.size > 3) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .offset(x = -(overlapOffset * 3))
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.fill.tertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+${colors.size - 3}",
                        color = NekkoTheme.colors.text.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
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
