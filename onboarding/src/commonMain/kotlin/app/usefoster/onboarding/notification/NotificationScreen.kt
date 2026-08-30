package app.usefoster.onboarding.notification

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.onboarding.components.StepIndicator
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.onboarding.notification.rememberNotificationPermissionLauncher
import app.usefoster.onboarding.presentation.rememberNotificationViewModel
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.checkin_img
import foster.onboarding.generated.resources.checkin_imglight
import org.jetbrains.compose.resources.painterResource
import foster.onboarding.generated.resources.action_skip
import foster.onboarding.generated.resources.cd_checkin_illustration
import foster.onboarding.generated.resources.error_profile_network
import foster.onboarding.generated.resources.error_profile_not_authenticated
import foster.onboarding.generated.resources.error_profile_not_found
import foster.onboarding.generated.resources.error_profile_server
import foster.onboarding.generated.resources.error_unexpected
import foster.onboarding.generated.resources.notif_get_notified
import foster.onboarding.generated.resources.notif_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationScreen(
    onNavigateToMainApp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberNotificationViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val profileErrorMessages = ProfileErrorMessages(
        notAuthenticated = stringResource(Res.string.error_profile_not_authenticated),
        profileNotFound = stringResource(Res.string.error_profile_not_found),
        network = stringResource(Res.string.error_profile_network),
        server = stringResource(Res.string.error_profile_server),
        unexpected = stringResource(Res.string.error_unexpected),
    )

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
                is NotificationEvent.ShowError -> snackbarHostState.showSnackbar(
                    profileErrorMessages.message(event.error),
                )
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
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

private data class ProfileErrorMessages(
    val notAuthenticated: String,
    val profileNotFound: String,
    val network: String,
    val server: String,
    val unexpected: String,
) {
    fun message(error: OnboardingProfileError): String = when (error) {
        OnboardingProfileError.NotAuthenticated -> notAuthenticated
        OnboardingProfileError.ProfileNotFound -> profileNotFound
        OnboardingProfileError.Network -> network
        OnboardingProfileError.Server -> server
        is OnboardingProfileError.Unknown -> unexpected
    }
}

@Composable
private fun NotificationScreenContent(
    state: NotificationState,
    onAction: (NotificationAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val painter =
        painterResource(Res.drawable.checkin_img)


    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FosterTheme.colors.background.b0,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    StepIndicator(
                        totalSteps = 7,
                        currentStep = 6,
                    )
                },
                navigationIcon = { },
                actions = {
//                    if (!state.isSubmitting) {
//                        Button(
//                            onClick = { onAction(NotificationAction.SkipClicked) },
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = FosterTheme.colors.background.b0
//                            )
//                        ) {
//                            Text(
//                                text = stringResource(Res.string.action_skip),
//                                fontSize = 17.sp,
//                                fontWeight = FontWeight.SemiBold,
//                                color = FosterTheme.colors.text.secondary,
//                            )
//                        }
//                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            AdaptiveSurface {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(12.dp))
                    FosterButton(
                        text = if (state.isNotificationEnabled) "Finish" else "Turn on Notification",
                        onClick = { onAction(NotificationAction.TurnOnClicked) },
                        modifier = Modifier.weight(1f),
                        loading = state.isSubmitting,
                    )
                }
            }
        },
        containerColor = FosterTheme.colors.background.b0
    ) { innerPadding ->
        AdaptiveSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(42.dp))
                Text(
                    text = stringResource(Res.string.notif_get_notified),
                    style = FosterTheme.typography.heading1Bold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = FosterTheme.colors.text.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.notif_subtitle),
                    style = FosterTheme.typography.heading3Bold,
                    fontWeight = FontWeight.Medium,
                    color = FosterTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painter,
                    contentDescription = stringResource(Res.string.cd_checkin_illustration),
                    contentScale = ContentScale.Fit
                )
            }
        }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewNotificationScreen() {
    FosterTheme {
        NotificationScreen(
            onNavigateToMainApp = {},
            onBack = {},
        )
    }
}
