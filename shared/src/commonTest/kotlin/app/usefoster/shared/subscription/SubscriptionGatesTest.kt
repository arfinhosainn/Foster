package app.usefoster.shared.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit tests for the free-tier gating logic — no RevenueCat, no Supabase,
 * no UI. Verifies the 5-contact cap, subscription-only brainstorm access, and that a
 * single `unlimited` entitlement unlocks BOTH gates.
 */
class SubscriptionGatesTest {

    // -- Contact gate (free = 5 max) -----------------------------------------

    @Test
    fun contactGate_allowsFreeUserBelowLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = false, currentContactCount = 4)
        assertEquals(GateResult.Allowed, result)
    }

    @Test
    fun contactGate_blocksFreeUserAtLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = false, currentContactCount = 5)
        assertTrue(result is GateResult.Blocked)
        val reason = (result as GateResult.Blocked).reason
        assertTrue(reason is BlockReason.ContactsLimitReached)
        assertEquals(5, (reason as BlockReason.ContactsLimitReached).limit)
    }

    @Test
    fun contactGate_blocksFreeUserAboveLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = false, currentContactCount = 15)
        assertTrue(result is GateResult.Blocked)
    }

    @Test
    fun contactGate_allowsSubscribedUserAtLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = true, currentContactCount = 5)
        assertEquals(GateResult.Allowed, result)
    }

    @Test
    fun contactGate_allowsSubscribedUserFarAboveLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = true, currentContactCount = 100)
        assertEquals(GateResult.Allowed, result)
    }

    // -- Brainstorm gate (subscription required) -----------------------------

    @Test
    fun brainstormGate_blocksFreeUserRegardlessOfGenerationCount() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = false, monthlyGenerationCount = 2)
        assertTrue(result is GateResult.Blocked)
        val reason = (result as GateResult.Blocked).reason
        assertEquals(BlockReason.BrainstormRequiresSubscription, reason)
    }

    @Test
    fun brainstormGate_allowsSubscribedUser() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = true, monthlyGenerationCount = 0)
        assertEquals(GateResult.Allowed, result)
    }

    // -- Single entitlement unlocks both -------------------------------------

    @Test
    fun singleEntitlementUnlocksBothGates() {
        assertEquals(
            GateResult.Allowed,
            SubscriptionGates.contactGate(isSubscribed = true, currentContactCount = 50),
        )
        assertEquals(
            GateResult.Allowed,
            SubscriptionGates.brainstormGate(isSubscribed = true, monthlyGenerationCount = 0),
        )
    }

    @Test
    fun freeLimitsAreCorrect() {
        assertEquals(5, SubscriptionGates.FREE_CONTACT_LIMIT)
    }

    @Test
    fun cancellationKeepsAccessUntilTheActiveEntitlementExpires() {
        assertTrue(isCancellationPending(isActive = true, unsubscribeDetectedAtMillis = 1L))
        assertTrue(!isCancellationPending(isActive = false, unsubscribeDetectedAtMillis = 1L))
        assertTrue(!isCancellationPending(isActive = true, unsubscribeDetectedAtMillis = null))
    }
}
