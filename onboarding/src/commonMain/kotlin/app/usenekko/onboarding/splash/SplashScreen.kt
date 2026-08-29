package app.usenekko.onboarding.splash

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
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.domain.OnboardingProfileError
import app.usenekko.onboarding.presentation.toUserMessageResource
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.action_try_again
import nekko.onboarding.generated.resources.error_profile_load_title
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
            .background(NekkoTheme.colors.background.b0),
        contentAlignment = Alignment.Center,
    ) {
        if (profileLoadError == null) {
            Text(
                text = "Nekko",
                style = NekkoTheme.typography.heading1Bold,
                color = NekkoTheme.colors.text.primary,
            )
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.error_profile_load_title),
                    style = NekkoTheme.typography.heading2Bold,
                    color = NekkoTheme.colors.text.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(profileLoadError.toUserMessageResource()),
                    style = NekkoTheme.typography.body,
                    color = NekkoTheme.colors.text.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                NekkoButton(
                    text = stringResource(Res.string.action_try_again),
                    onClick = onRetry,
                )
            }
        }
    }
}