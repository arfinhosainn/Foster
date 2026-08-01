package app.usenekko.home

import androidx.compose.foundation.background
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
import app.usenekko.designsystem.buttons.AudienceOption
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.AmbientGlow
import app.usenekko.designsystem.navbar.bottom.bottomNavBar.GlassBottomNavBar
import app.usenekko.designsystem.navbar.top.NekkoTopBar
import app.usenekko.designsystem.shapes.SawToothCircleShape
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

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {

    val options = listOf(
        AudienceOption("Everyone", nekko.home.generated.resources.Res.drawable.ic_group),
        AudienceOption("Family", nekko.home.generated.resources.Res.drawable.ic_family),
        AudienceOption("Friends", nekko.home.generated.resources.Res.drawable.ic_friends),
        AudienceOption("Acquaintance", nekko.home.generated.resources.Res.drawable.ic_acquaintance),
        AudienceOption("Others", nekko.home.generated.resources.Res.drawable.ic_person),
    )
    var selected by remember { mutableStateOf(options.first()) }
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
                selectedAudience = selected,
                onAudienceSelect = { selected = it },
                userName = "Jane Bell",
                onAvatarClick = {},
                onPremiumClick = {},
            )

        }, bottomBar = {
            GlassBottomNavBar(
                // EFFECT, sibling
                selectedIndex = 1,
                onItemSelected = {},
                onAddClick = {},
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
                    outstandingCount = 0,
                    upToDateCount = 0,
                    outstandingBgResource = Res.drawable.ic_globe,
                    upToDateBgResource = Res.drawable.ic_fire,
                    gradientOrbResource = Res.drawable.img_gradientss
                )

                Spacer(Modifier.height(70.dp))

//                Box(
//                    modifier = Modifier
//                        .size(200.dp)
//                        .clip(SawToothCircleShape())
//                        .background(NekkoTheme.colors.background.b2),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = null,
//                        tint = NekkoTheme.colors.text.tertiary,
//                        modifier = Modifier.size(50.dp)
//                    )
//                }
//                Spacer(Modifier.height(30.dp))
//
//                Text(
//                    "Get started",
//                    color = NekkoTheme.colors.text.primary,
//                    fontSize = 24.sp,
//                    fontWeight = FontWeight.Medium
//                )
//                Spacer(Modifier.height(10.dp))
//                Text(
//                    "Import from your contact",
//                    color = NekkoTheme.colors.text.tertiary,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium
//                )
                CheckInTimelineGridSample()
            }

        }
    }


}


@PreviewLightDark
@Composable
fun PreviewHomeScreen() {
    NekkoTheme {
        HomeScreen()
    }
}
