package app.usefoster.shared.subscription

import androidx.compose.runtime.Composable

/**
 * Last-known entitlement data for a single user, persisted locally so the app
 * can fail OPEN when RevenueCat is unreachable.
 *
 * Scenario this exists for: a paying customer launches the app on a plane /
 * flaky network. [SubscriptionRepository.refresh] fails, and without a
 * last-known record we would fall back to "not subscribed" and lock the very
 * features they pay for — a 1-star generator. Instead we seed the live
 * entitlement from this snapshot (per user), then the first successful
 * [SubscriptionRepository.refresh] overwrites it with authoritative state.
 *
 * Snapshots are always keyed by the authenticated user id and only written
 * from real server results ([CustomerInfo] updates), never from sign-outs —
 * so a spurious offline "not authenticated" can never wipe the fail-open data.
 */
data class EntitlementSnapshot(
    /** Last known state of the `unlimited` entitlement. */
    val subscribed: Boolean,
    /** Whether the active subscription was canceled and will not renew. */
    val cancellationPending: Boolean = false,
    /** Store product id that granted the entitlement (for the manage deep link). */
    val productId: String?,
    /** Platform-correct subscription management URL from RevenueCat. */
    val managementUrl: String?,
)

/** Local persistence for [EntitlementSnapshot] (preferences on both platforms). */
interface EntitlementSnapshotStore {
    /** Last saved snapshot for [userId], or null when nothing was ever stored. */
    suspend fun load(userId: String): EntitlementSnapshot?

    /** Overwrite the snapshot for [userId]. */
    suspend fun save(userId: String, snapshot: EntitlementSnapshot)
}

/** Platform-specific persistence (DataStore preferences / NSUserDefaults). */
@Composable
expect fun rememberEntitlementSnapshotStore(): EntitlementSnapshotStore