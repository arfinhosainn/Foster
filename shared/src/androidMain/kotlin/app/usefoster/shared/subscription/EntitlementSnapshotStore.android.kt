package app.usefoster.shared.subscription

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * DataStore-backed [EntitlementSnapshotStore]. One record per user id so
 * switching accounts never leaks one user's entitlement into another's.
 * Keys are suffixed with the user id (a Supabase UUID) and stored in the
 * same preferences DataStore used by the paywall-gate state.
 */
private val Context.entitlementSnapshotStore by preferencesDataStore(name = "entitlement_snapshot")

private class DataStoreEntitlementSnapshotStore(
    private val dataStore: DataStore<Preferences>,
) : EntitlementSnapshotStore {

    private fun subscribedKey(userId: String) = booleanPreferencesKey("entitlement_snapshot_$userId" + "_subscribed")
    private fun cancellationPendingKey(userId: String) = booleanPreferencesKey("entitlement_snapshot_$userId" + "_cancellation_pending")
    private fun productKey(userId: String) = stringPreferencesKey("entitlement_snapshot_$userId" + "_product")
    private fun urlKey(userId: String) = stringPreferencesKey("entitlement_snapshot_$userId" + "_url")

    override suspend fun load(userId: String): EntitlementSnapshot? {
        val prefs = dataStore.data.first()
        val subscribed = prefs[subscribedKey(userId)] ?: return null
        return EntitlementSnapshot(
            subscribed = subscribed,
            cancellationPending = prefs[cancellationPendingKey(userId)] ?: false,
            productId = prefs[productKey(userId)],
            managementUrl = prefs[urlKey(userId)],
        )
    }

    override suspend fun save(userId: String, snapshot: EntitlementSnapshot) {
        dataStore.edit { prefs ->
            prefs[subscribedKey(userId)] = snapshot.subscribed
            prefs[cancellationPendingKey(userId)] = snapshot.cancellationPending
            snapshot.productId?.let { prefs[productKey(userId)] = it }
            snapshot.managementUrl?.let { prefs[urlKey(userId)] = it }
        }
    }
}

@Composable
actual fun rememberEntitlementSnapshotStore(): EntitlementSnapshotStore {
    val context = LocalContext.current
    return remember {
        DataStoreEntitlementSnapshotStore(context.entitlementSnapshotStore)
    }
}