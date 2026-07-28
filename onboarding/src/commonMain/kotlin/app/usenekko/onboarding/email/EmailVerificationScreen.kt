package app.usenekko.onboarding.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.VerificationCodeField
import app.usenekko.onboarding.presentation.rememberEmailVerificationViewModel
import app.usenekko.theme.NekkoTheme

@Composable
fun EmailVerificationScreen(
    email: String,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberEmailVerificationViewModel(email)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EmailVerificationEvent.NavigateToNext -> onNavigateToNext()
                EmailVerificationEvent.NavigateBack -> onBack()
                EmailVerificationEvent.NavigateSkip -> onSkip()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b1)
            .imePadding()
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = { },
                    navigationIcon = { },
                    actions = {
                        Button(
                            onClick = { viewModel.onAction(EmailVerificationAction.SkipClicked) },
                            colors = ButtonDefaults.buttonColors(containerColor = NekkoTheme.colors.background.b0)
                        ) {
                            Text(
                                text = "Skip",
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
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NekkoButton(
                        text = "Back",
                        onClick = { viewModel.onAction(EmailVerificationAction.BackClicked) },
                        modifier = Modifier.weight(0.2f),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NekkoTheme.colors.background.b1,
                            )
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    NekkoButton(
                        text = "Next",
                        onClick = {
                            if (!state.isVerifying) {
                                viewModel.onAction(EmailVerificationAction.VerifyClicked)
                            }
                        },
                        modifier = Modifier.weight(0.8f),
                    )
                }
            },
            containerColor = NekkoTheme.colors.background.b0
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Verification code sent\nto $email",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = NekkoTheme.colors.text.primary,
                )

                Spacer(Modifier.height(16.dp))

                VerificationCodeField(
                    code = state.code,
                    onCodeChange = { viewModel.onAction(EmailVerificationAction.CodeChanged(it)) },
                    isLoading = state.isVerifying,
                    onDone = { viewModel.onAction(EmailVerificationAction.Done) },
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewEmailVerificationScreen() {
    NekkoTheme {
        EmailVerificationScreen(
            email = "user@example.com",
            onNavigateToNext = {},
            onBack = {},
            onSkip = {},
        )
    }
}
