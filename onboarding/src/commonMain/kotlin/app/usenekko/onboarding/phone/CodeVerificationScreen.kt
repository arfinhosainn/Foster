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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.topbar.NekkoTopAppBar
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b1)
            .imePadding()
    ) {
        NekkoTopAppBar(
            trailingContent = {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip",
                        color = NekkoTheme.colors.text.primary,
                        fontSize = 16.sp,
                    )
                }
            }
        ) {
            StepIndicator(
                totalSteps = 4,
                currentStep = 1,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
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
