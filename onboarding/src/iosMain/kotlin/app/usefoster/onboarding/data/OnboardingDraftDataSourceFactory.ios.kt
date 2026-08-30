package app.usefoster.onboarding.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource

@Composable
actual fun rememberOnboardingDraftDataSource(): OnboardingDraftLocalDataSource {
    return remember {
        NSUserDefaultsOnboardingDraftDataSource()
    }
}
