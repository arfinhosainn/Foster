package app.usenekko.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.onboarding.data.rememberOnboardingDraftDataSource

val LocalOnboardingDraftStore = staticCompositionLocalOf<OnboardingDraftStore> {
    error("No OnboardingDraftStore provided. Wrap your content with LocalOnboardingDraftStore.")
}

@Composable
fun OnboardingDraftStoreProvider(
    content: @Composable () -> Unit,
) {
    val dataSource = rememberOnboardingDraftDataSource()
    val draftStore = remember { OnboardingDraftStore(dataSource) }

    CompositionLocalProvider(LocalOnboardingDraftStore provides draftStore) {
        content()
    }
}
