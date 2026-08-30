package app.usefoster.shared.paywall

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.paywallGateDataStore by preferencesDataStore(name = "paywall_gate")

private class DataStorePaywallGateDataSource(
    private val dataStore: DataStore<Preferences>,
) : PaywallGateDataSource {

    private object Keys {
        val IsSubscribed = booleanPreferencesKey("paywall_gate_is_subscribed")
        val LaunchSessionId = intPreferencesKey("paywall_gate_launch_session_id")
        val RegularDismissedSessionId = intPreferencesKey("paywall_gate_regular_dismissed_session")
        val DiscountLastShownAt = longPreferencesKey("paywall_gate_discount_last_shown_at")
        val OfferExpiresAt = longPreferencesKey("paywall_gate_offer_expires_at")
    }

    override suspend fun getState(): PaywallGateState {
        val prefs = dataStore.data.first()
        return PaywallGateState(
            isSubscribed = prefs[Keys.IsSubscribed] ?: false,
            launchSessionId = prefs[Keys.LaunchSessionId] ?: 0,
            regularPaywallDismissedSessionId = prefs[Keys.RegularDismissedSessionId],
            discountPaywallLastShownAt = prefs[Keys.DiscountLastShownAt],
            discountOfferExpiresAt = prefs[Keys.OfferExpiresAt],
        )
    }

    override suspend fun setState(state: PaywallGateState) {
        dataStore.edit { prefs ->
            prefs[Keys.IsSubscribed] = state.isSubscribed
            prefs[Keys.LaunchSessionId] = state.launchSessionId
            state.regularPaywallDismissedSessionId?.let {
                prefs[Keys.RegularDismissedSessionId] = it
            } ?: prefs.remove(Keys.RegularDismissedSessionId)
            state.discountPaywallLastShownAt?.let {
                prefs[Keys.DiscountLastShownAt] = it
            } ?: prefs.remove(Keys.DiscountLastShownAt)
            state.discountOfferExpiresAt?.let {
                prefs[Keys.OfferExpiresAt] = it
            } ?: prefs.remove(Keys.OfferExpiresAt)
        }
    }
}

@Composable
actual fun rememberPaywallGateDataSource(): PaywallGateDataSource {
    val context = LocalContext.current
    return remember {
        DataStorePaywallGateDataSource(context.paywallGateDataStore)
    }
}
