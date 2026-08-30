package app.usefoster.onboarding.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.onboarding.presentation.toUserMessageResource
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.action_try_again
import foster.onboarding.generated.resources.error_profile_load_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    profileLoadError: OnboardingProfileError? = null,
    onRetry: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FosterTheme.colors.background.b0),
        contentAlignment = Alignment.Center,
    ) {
        if (profileLoadError == null) {
            Text(
                text = "Foster",
                style = FosterTheme.typography.heading1Bold,
                color = FosterTheme.colors.text.primary,
            )
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.error_profile_load_title),
                    style = FosterTheme.typography.heading2Bold,
                    color = FosterTheme.colors.text.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(profileLoadError.toUserMessageResource()),
                    style = FosterTheme.typography.body,
                    color = FosterTheme.colors.text.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                FosterButton(
                    text = stringResource(Res.string.action_try_again),
                    onClick = onRetry,
                )
            }
        }
    }
}