package app.usenekko.onboarding.data

import androidx.compose.runtime.Composable
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource

@Composable
expect fun rememberOnboardingDraftDataSource(): OnboardingDraftLocalDataSource
