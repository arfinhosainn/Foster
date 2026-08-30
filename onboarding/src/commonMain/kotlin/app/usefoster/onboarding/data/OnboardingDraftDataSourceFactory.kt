package app.usefoster.onboarding.data

import androidx.compose.runtime.Composable
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource

@Composable
expect fun rememberOnboardingDraftDataSource(): OnboardingDraftLocalDataSource
