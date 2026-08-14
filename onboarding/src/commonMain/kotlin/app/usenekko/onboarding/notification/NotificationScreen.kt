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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.notification.rememberNotificationPermissionLauncher
import app.usenekko.onboarding.presentation.rememberNotificationViewModel
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.checkin_img
import nekko.onboarding.generated.resources.checkin_imglight
import org.jetbrains.compose.resources.painterResource

@Composable
fun NotificationScreen(
    onNavigateToMainApp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberNotificationViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val requestNotificationPermission = rememberNotificationPermissionLauncher(
        onGranted = {
            viewModel.onAction(NotificationAction.PermissionResult(granted = true))
        },
        onDenied = {
            viewModel.onAction(NotificationAction.PermissionResult(granted = false))
        },
        onStatusChanged = { enabled ->
            viewModel.onAction(NotificationAction.PermissionStateChanged(enabled))
        },
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                NotificationEvent.NavigateToMainApp -> onNavigateToMainApp()
                is NotificationEvent.ShowError -> { /* snackbar or error UI */ }
            }
        }
    }

    NotificationScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                NotificationAction.TurnOnClicked -> {
                    requestNotificationPermission()
                }
                is NotificationAction.PermissionStateChanged -> viewModel.onAction(action)
                is NotificationAction.PermissionResult -> viewModel.onAction(action)
                NotificationAction.SkipClicked -> {
                    viewModel.onAction(NotificationAction.SkipClicked)
                }
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun NotificationScreenContent(
    state: NotificationState,
    onAction: (NotificationAction) -> Unit,
    onBack: () -> Unit,
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
                    if (!state.isSubmitting) {
                        Button(
                            onClick = { onAction(NotificationAction.SkipClicked) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NekkoTheme.colors.background.b0
                            )
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NekkoTheme.colors.text.secondary,
                            )
                        }
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
                    text = if (state.isNotificationEnabled) "Finish" else "Turn on Notification",
                    onClick = { onAction(NotificationAction.TurnOnClicked) },
                    modifier = Modifier.weight(1f),
                    loading = state.isSubmitting,
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
            onNavigateToMainApp = {},
            onBack = {},
        )
    }
}
