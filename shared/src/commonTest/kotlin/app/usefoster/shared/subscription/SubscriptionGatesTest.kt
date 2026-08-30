package app.usefoster.shared.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit tests for the free-tier gating logic — no RevenueCat, no Supabase,
 * no UI. Verifies the 10-contact cap, the 3/month brainstorm cap, and that a
 * single `unlimited` entitlement unlocks BOTH gates.
 */
class SubscriptionGatesTest {

    // -- Contact gate (free = 10 max) ----------------------------------------

    @Test
    fun contactGate_allowsFreeUserBelowLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = false, currentContactCount = 9)
        assertEquals(GateResult.Allowed, result)
    }

    @Test
    fun contactGate_blocksFreeUserAtLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = false, currentContactCount = 10)
        assertTrue(result is GateResult.Blocked)
        val reason = (result as GateResult.Blocked).reason
        assertTrue(reason is BlockReason.ContactsLimitReached)
        assertEquals(10, (reason as BlockReason.ContactsLimitReached).limit)
    }

    @Test
    fun contactGate_blocksFreeUserAboveLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = false, currentContactCount = 15)
        assertTrue(result is GateResult.Blocked)
    }

    @Test
    fun contactGate_allowsSubscribedUserAtLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = true, currentContactCount = 10)
        assertEquals(GateResult.Allowed, result)
    }

    @Test
    fun contactGate_allowsSubscribedUserFarAboveLimit() {
        val result = SubscriptionGates.contactGate(isSubscribed = true, currentContactCount = 100)
        assertEquals(GateResult.Allowed, result)
    }

    // -- Brainstorm gate (free = 3/month) ------------------------------------

    @Test
    fun brainstormGate_allowsFreeUserBelowLimit() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = false, monthlyGenerationCount = 2)
        assertEquals(GateResult.Allowed, result)
    }

    @Test
    fun brainstormGate_blocksFreeUserAtLimit() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = false, monthlyGenerationCount = 3)
        assertTrue(result is GateResult.Blocked)
        val reason = (result as GateResult.Blocked).reason
        assertTrue(reason is BlockReason.BrainstormLimitReached)
        assertEquals(3, (reason as BlockReason.BrainstormLimitReached).limit)
    }

    @Test
    fun brainstormGate_blocksFreeUserAboveLimit() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = false, monthlyGenerationCount = 5)
        assertTrue(result is GateResult.Blocked)
    }

    @Test
    fun brainstormGate_allowsSubscribedUserAtLimit() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = true, monthlyGenerationCount = 3)
        assertEquals(GateResult.Allowed, result)
    }

    @Test
    fun brainstormGate_allowsSubscribedUserFarAboveLimit() {
        val result = SubscriptionGates.brainstormGate(isSubscribed = true, monthlyGenerationCount = 100)
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
            SubscriptionGates.brainstormGate(isSubscribed = true, monthlyGenerationCount = 50),
        )
    }

    @Test
    fun freeLimitsAreCorrect() {
        assertEquals(10, SubscriptionGates.FREE_CONTACT_LIMIT)
        assertEquals(3, SubscriptionGates.FREE_BRAINSTORM_MONTHLY_LIMIT)
    }
}
