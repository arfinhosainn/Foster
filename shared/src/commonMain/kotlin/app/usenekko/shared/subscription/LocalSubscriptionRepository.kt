package app.usenekko.shared.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide access to [SubscriptionRepository]. Provided once in
 * [OnboardingDraftStoreProvider] alongside the other data sources.
 */
val LocalSubscriptionRepository = staticCompositionLocalOf<SubscriptionRepository> {
    error("SubscriptionRepository not provided")
}
