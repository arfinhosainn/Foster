package app.usenekko.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import app.usenekko.home.presentation.components.CheckInTimelineGridSample
import app.usenekko.home.presentation.components.StatusSummaryCard
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState
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
                    .padding(horizontal = 24.dp),
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
                    CheckInTimelineGridSample()
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


@PreviewLightDark
@Composable
fun PreviewHomeScreen() {
    NekkoTheme {
        HomeScreen()
    }
}
