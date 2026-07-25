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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.topbar.NekkoTopAppBar
import app.usenekko.onboarding.components.PhoneNumberField
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.theme.NekkoTheme

@Composable
fun PhoneScreen(
    onNavigateToCodeVerification: (phoneNumber: String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
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
                currentStep = 0,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            PhoneNumberField(
                phoneNumber = phone,
                onPhoneNumberChange = { phone = it },
                onDone = { fullNumber -> onNavigateToCodeVerification(fullNumber) },
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
                onClick = { onNavigateToCodeVerification("+60$phone") },
                modifier = Modifier.weight(0.8f),
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NekkoTheme.typography.heading3Bold.fontFamily,
                ),
            )
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
