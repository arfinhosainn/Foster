package app.usenekko.onboarding.phone

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.PhoneNumberField
import app.usenekko.onboarding.presentation.LocalOnboardingDraftStore
import app.usenekko.theme.NekkoTheme

@Composable
fun PhoneScreen(
    onNavigateToCodeVerification: (phoneNumber: String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draftStore = LocalOnboardingDraftStore.current
    val viewModel = viewModel { PhoneViewModel(draftStore) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PhoneEvent.NavigateBack -> onBack()
                PhoneEvent.NavigateSkip -> onSkip()
                is PhoneEvent.NavigateToCodeVerification -> {
                    onNavigateToCodeVerification(event.phoneNumber)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
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
                            onClick = { viewModel.onAction(PhoneAction.SkipClicked) },
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
                        onClick = { viewModel.onAction(PhoneAction.BackClicked) },
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
                        onClick = { viewModel.onAction(PhoneAction.ContinueClicked) },
                        modifier = Modifier.weight(0.8f),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NekkoTheme.typography.heading3Bold.fontFamily,
                        ),
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

                PhoneNumberField(
                    phoneNumber = state.phoneNumber,
                    onPhoneNumberChange = { viewModel.onAction(PhoneAction.PhoneNumberChanged(it)) },
                    onDone = { viewModel.onAction(PhoneAction.ContinueClicked) },
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewPhoneScreen() {
    NekkoTheme {
        PhoneScreen(
            onNavigateToCodeVerification = {},
            onBack = {},
            onSkip = {},
        )
    }
}
