package app.usefoster.shared.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

/**
 * NSUserDefaults-backed [EntitlementSnapshotStore]. Same record-per-user
 * semantics as the Android DataStore implementation so the whole entitlement
 * snapshot survives restarts and account switches.
 */
private class NSUserDefaultsEntitlementSnapshotStore : EntitlementSnapshotStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    private fun subscribedKey(userId: String) = "entitlement_snapshot_$userId" + "_subscribed"
    private fun cancellationPendingKey(userId: String) = "entitlement_snapshot_$userId" + "_cancellation_pending"
    private fun productKey(userId: String) = "entitlement_snapshot_$userId" + "_product"
    private fun urlKey(userId: String) = "entitlement_snapshot_$userId" + "_url"

    private fun nullableBool(key: String): Boolean? =
        if (defaults.objectForKey(key) == null) null else defaults.boolForKey(key)

    private fun nullableString(key: String): String? =
        if (defaults.objectForKey(key) == null) null else defaults.stringForKey(key)

    override suspend fun load(userId: String): EntitlementSnapshot? {
        val subscribed = nullableBool(subscribedKey(userId)) ?: return null
        return EntitlementSnapshot(
            subscribed = subscribed,
            cancellationPending = nullableBool(cancellationPendingKey(userId)) ?: false,
            productId = nullableString(productKey(userId)),
            managementUrl = nullableString(urlKey(userId)),
        )
    }

    override suspend fun save(userId: String, snapshot: EntitlementSnapshot) {
        defaults.setBool(snapshot.subscribed, forKey = subscribedKey(userId))
        defaults.setBool(snapshot.cancellationPending, forKey = cancellationPendingKey(userId))
        snapshot.productId?.let { defaults.setObject(it, forKey = productKey(userId)) }
        snapshot.managementUrl?.let { defaults.setObject(it, forKey = urlKey(userId)) }
        defaults.synchronize()
    }
}

@Composable
actual fun rememberEntitlementSnapshotStore(): EntitlementSnapshotStore {
    return remember { NSUserDefaultsEntitlementSnapshotStore() }
}