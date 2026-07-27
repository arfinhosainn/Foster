package app.usenekko.onboarding.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.checkin_img
import nekko.onboarding.generated.resources.checkin_imglight
import org.jetbrains.compose.resources.painterResource

@Composable
fun NotificationScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(NotificationState()) }

    NotificationScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is NotificationAction.TurnOnClicked -> {
                    state = state.copy(isNotificationEnabled = true)
                }
            }
        },
        onNavigateToNext = onNavigateToNext,
        onBack = onBack,
        onSkip = onSkip,
        modifier = modifier,
    )
}

@Composable
private fun NotificationScreenContent(
    state: NotificationState,
    onAction: (NotificationAction) -> Unit,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val painter = if (isSystemInDarkTheme()) {
        painterResource(Res.drawable.checkin_img)
    } else {
        painterResource(Res.drawable.checkin_imglight)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NekkoTheme.colors.background.b0,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    StepIndicator(
                        totalSteps = 8,
                        currentStep = 7,
                    )
                },
                navigationIcon = { },
                actions = {
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.buttonColors(containerColor = NekkoTheme.colors.background.b0)
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NekkoTheme.colors.text.secondary,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 24.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(12.dp))
                NekkoButton(
                    text = "Turn on Notification",
                    onClick = onNavigateToNext,
                    modifier = Modifier.weight(1f),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(42.dp))
                Text(
                    text = "Get Notified",
                    style = NekkoTheme.typography.heading1Bold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Keep up with check-ins &\nbirthdays with friends",
                    style = NekkoTheme.typography.heading3Bold,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Check-in illustration",
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


@PreviewLightDark
@Composable
fun PreviewNotificationScreen() {
    NekkoTheme {
        NotificationScreen(
            onNavigateToNext = {},
            onBack = {},
            onSkip = {},
        )
    }
}
