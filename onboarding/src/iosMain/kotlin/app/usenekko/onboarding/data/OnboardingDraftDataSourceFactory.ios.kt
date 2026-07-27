package app.usenekko.onboarding.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource

@Composable
actual fun rememberOnboardingDraftDataSource(): OnboardingDraftLocalDataSource {
    return remember {
        NSUserDefaultsOnboardingDraftDataSource()
    }
}
