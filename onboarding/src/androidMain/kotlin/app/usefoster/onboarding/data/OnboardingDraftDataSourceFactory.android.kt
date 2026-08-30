package app.usefoster.onboarding.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.preferencesDataStore
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import android.content.Context

private val Context.onboardingDataStore by preferencesDataStore(
    name = "onboarding_preferences"
)

@Composable
actual fun rememberOnboardingDraftDataSource(): OnboardingDraftLocalDataSource {
    val context = LocalContext.current
    return remember {
        DataStoreOnboardingDraftDataSource(
            dataStore = context.onboardingDataStore,
            json = onboardingJson,
        )
    }
}
