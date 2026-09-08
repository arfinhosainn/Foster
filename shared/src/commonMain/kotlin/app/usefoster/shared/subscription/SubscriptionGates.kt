package app.usefoster.shared.subscription

/**
 * Pure, side-effect-free subscription gating logic.
 *
 * Kept here (not in the ViewModels) so the free-tier limits are unit-testable
 * without any UI / RevenueCat / Supabase wiring. A single `unlimited`
 * entitlement unlocks BOTH gates.
 *
 * Free limits:
 *  - Contacts:     5 total
 *  - Brainstorm:   subscription required
 */
object SubscriptionGates {
    const val FREE_CONTACT_LIMIT = 5

    fun contactGate(isSubscribed: Boolean, currentContactCount: Int): GateResult =
        if (isSubscribed || currentContactCount < FREE_CONTACT_LIMIT) {
            GateResult.Allowed
        } else {
            GateResult.Blocked(BlockReason.ContactsLimitReached(FREE_CONTACT_LIMIT))
        }

    fun brainstormGate(isSubscribed: Boolean, monthlyGenerationCount: Int): GateResult =
        if (isSubscribed) {
            GateResult.Allowed
        } else {
            GateResult.Blocked(BlockReason.BrainstormRequiresSubscription)
        }
}

sealed interface GateResult {
    data object Allowed : GateResult
    data class Blocked(val reason: BlockReason) : GateResult
}

sealed interface BlockReason {
    data class ContactsLimitReached(val limit: Int) : BlockReason
    data class BrainstormLimitReached(val limit: Int) : BlockReason
    data object BrainstormRequiresSubscription : BlockReason
}
