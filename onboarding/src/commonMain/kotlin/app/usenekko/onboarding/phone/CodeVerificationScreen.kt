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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.PhoneNumberField
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.components.VerificationCodeField
import app.usenekko.theme.NekkoTheme

@Composable
fun CodeVerificationScreen(
    phoneNumber: String,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

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
                    title = {
                        StepIndicator(
                            totalSteps = 4,
                            currentStep = 1,
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
                        onClick = onBack,
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
                            if (!isVerifying) {
                                isVerifying = true
                                onNavigateToNext()
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

                PhoneNumberField(
                    phoneNumber = phoneNumber.removePrefix("+60"),
                    onPhoneNumberChange = {},
                    isConfirmed = true,
                )

                Spacer(Modifier.height(16.dp))

                VerificationCodeField(
                    code = code,
                    onCodeChange = { code = it },
                    isLoading = isVerifying,
                    onDone = {
                        isVerifying = true
                        onNavigateToNext()
                    },
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewCodeVerificationScreen() {
    NekkoTheme {
        CodeVerificationScreen(
            phoneNumber = "+60123456789",
            onNavigateToNext = {},
            onBack = {},
            onSkip = {},
        )
    }
}
