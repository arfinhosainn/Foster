package app.usenekko.onboarding.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.shapes.SawToothCircleShape
import app.usenekko.designsystem.topbar.NekkoTopAppBar
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.theme.NekkoTheme
import app.usenekko.designsystem.modifiers.glass
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_add
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource


/**
 * Group onboarding screen — allows the user to add themselves to existing
 * groups or create new ones before completing onboarding.
 */
@Composable
fun GroupScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Default sample groups matching the design
    var groups by remember {
        mutableStateOf(
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
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
    ) {
        // ── Top bar with step indicator ─────────────────────────────────
        NekkoTopAppBar {
            StepIndicator(
                totalSteps = 4,
                currentStep = 3,
            )
        }

        // ── Title section ───────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Add to a group",
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = NekkoTheme.colors.text.primary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Add user to a group",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.tertiary,
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Group cards grid ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            groups.forEach { group ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GroupCard(
                        group = group,
                        onClick = { /* toggle selection */ },
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = group.name,
                        style = NekkoTheme.typography.heading4Semibold,
                        color = NekkoTheme.colors.text.primary,
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "${group.members.size} people",
                        style = NekkoTheme.typography.footnote,
                        color = NekkoTheme.colors.text.tertiary,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ── "Create new group" prompt ───────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .glass(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = NekkoTheme.colors.fill.secondary,
                        borderColor = NekkoTheme.colors.stroke.primary,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* handle create group */ },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_add),
                    contentDescription = "Create new group",
                    tint = NekkoTheme.colors.background.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Wanna create a new group?",
                color = NekkoTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium

            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Tap on the plus button",
                color = NekkoTheme.colors.text.tertiary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Bottom navigation buttons ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NekkoTheme.colors.fill.tertiary,
                    contentColor = Color(0xFFFFFFFF)
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_back),
                    contentDescription = "Back",
                )
            }
            Spacer(Modifier.width(12.dp))
            NekkoButton(
                text = "Next",
                onClick = onNavigateToNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}


@Composable
private fun GroupCard(
    group: Group,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(160.dp)
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
            // Empty group → show a "+" icon
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add members",
                tint = NekkoTheme.colors.text.quaternary,
                modifier = Modifier.size(40.dp),
            )
        } else {
            // Show overlapping member avatar circles
            MemberAvatars(colors = group.members)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlapping member circles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MemberAvatars(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val avatarSize = 56.dp
    val overlapOffset = 18.dp  // how much each avatar slides into the previous one

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row {
            colors.forEachIndexed { index, color ->
                // Use a colored circle with a thin border to mimic the design's avatars.
                // Each successive avatar is shifted left to create the overlap.
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
                    // Inner colored "drop" avatar
                    Box(
                        modifier = Modifier
                            .size(avatarSize - 4.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Teardrop / blob shape approximated with a circle fill
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

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
