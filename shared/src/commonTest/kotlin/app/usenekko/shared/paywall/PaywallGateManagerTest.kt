package app.usenekko.shared.paywall

import app.usenekko.shared.domain.Result
import app.usenekko.shared.subscription.PaywallOffering
import app.usenekko.shared.subscription.PaywallPackage
import app.usenekko.shared.subscription.PurchaseOutcome
import app.usenekko.shared.subscription.SubscriptionError
import app.usenekko.shared.subscription.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PaywallGateManagerTest {

    private class FakeDataSource(initial: PaywallGateState = PaywallGateState()) : PaywallGateDataSource {
        var stored: PaywallGateState = initial
        var writeCount: Int = 0

        override suspend fun getState(): PaywallGateState = stored

        override suspend fun setState(state: PaywallGateState) {
            stored = state
            writeCount++
        }
    }

    private class FakeClock(var nowMillis: Long = 0L) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
    }

    private class FakeSubscriptionRepository(initiallySubscribed: Boolean = false) : SubscriptionRepository {
        private val subscribedFlow = MutableStateFlow(initiallySubscribed)
        override val isSubscribed = subscribedFlow.asStateFlow()

        fun becomeSubscribed() {
            subscribedFlow.value = true
        }

        fun setSubscribed(value: Boolean) {
            subscribedFlow.value = value
        }

        override suspend fun refresh(): Result<Unit, SubscriptionError> = Result.Success(Unit)

        override suspend fun loadPaywallOffering(): Result<PaywallOffering, SubscriptionError> {
            return Result.Success(PaywallOffering(monthly = null, annual = null))
        }

        override suspend fun purchase(pkg: PaywallPackage): PurchaseOutcome = PurchaseOutcome.Success

        override suspend fun restorePurchases(): Result<Boolean, SubscriptionError> =
            Result.Success(false)
    }

    private fun manager(
        dataSource: FakeDataSource,
        repository: FakeSubscriptionRepository,
        clock: FakeClock,
    ): PaywallGateManager = PaywallGateManager(
        dataSource = dataSource,
        subscriptionRepository = repository,
        clock = clock,
        backgroundDispatcher = Dispatchers.Unconfined,
    )

    private suspend fun awaitTrue(flow: kotlinx.coroutines.flow.StateFlow<Boolean>) {
        withTimeout(3_000) {
            while (!flow.value) delay(5)
        }
    }

    @Test
    fun triggerBeforeAppStartIsIgnored() = runBlocking {
        val source = FakeDataSource()
        val gate = manager(source, FakeSubscriptionRepository(), FakeClock())

        assertFalse(gate.reportTrigger(PaywallTrigger.LIMIT_HIT))
    }

    @Test
    fun limitHitShowsAndStampsDeadlineOnce() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 1_000L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()
        assertTrue(gate.reportTrigger(PaywallTrigger.LIMIT_HIT))
        assertTrue(gate.shouldShowDiscountPaywall.value)

        val expectedDeadline = 1_000L + (12.hours + 29.minutes).inWholeMilliseconds
        assertEquals(expectedDeadline, gate.offerExpiresAtMillis.value)
        assertEquals(expectedDeadline, source.stored.discountOfferExpiresAt)
        assertEquals(1_000L, source.stored.discountPaywallLastShownAt)

        gate.consumeShow()
        assertFalse(gate.shouldShowDiscountPaywall.value)
    }

    @Test
    fun subscribedUsersNeverSeeTheOffer() = runBlocking {
        val source = FakeDataSource()
        val gate = manager(source, FakeSubscriptionRepository(initiallySubscribed = true), FakeClock())

        gate.onAppStart()

        assertFalse(gate.reportTrigger(PaywallTrigger.LIMIT_HIT))
        assertFalse(gate.reportTrigger(PaywallTrigger.AHA_MOMENT))
        assertFalse(gate.reportTrigger(PaywallTrigger.WIN_BACK))
        assertFalse(gate.shouldShowDiscountPaywall.value)
    }

    @Test
    fun purchaseKillsTheOfferForever() = runBlocking {
        val source = FakeDataSource()
        val repo = FakeSubscriptionRepository()
        val clock = FakeClock(nowMillis = 0L)
        val gate = manager(source, repo, clock)

        gate.onAppStart()
        assertTrue(gate.reportTrigger(PaywallTrigger.ABANDONED_CHECKOUT))

        // RevenueCat entitlement flips after a successful purchase...
        repo.becomeSubscribed()
        withTimeout(3_000) {
            while (!source.stored.isSubscribed) delay(5)
        }
        assertFalse(gate.shouldShowDiscountPaywall.value)

        // ...and stays sticky even if entitlement later reads false offline.
        val stickyFlag = source.stored.isSubscribed
        assertTrue(stickyFlag)

        val restartedGate = manager(source, FakeSubscriptionRepository(initiallySubscribed = false), clock)
        restartedGate.onAppStart()
        clock.nowMillis += 30.days.inWholeMilliseconds
        assertFalse(restartedGate.reportTrigger(PaywallTrigger.WIN_BACK))
    }

    @Test
    fun cooldownBlocksRepeatImpressions() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 0L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()
        assertTrue(gate.reportTrigger(PaywallTrigger.LIMIT_HIT))
        gate.consumeShow()

        // Well inside the 14-day cooldown (and the offer window).
        clock.nowMillis += 2.days.inWholeMilliseconds
        assertFalse(gate.reportTrigger(PaywallTrigger.AHA_MOMENT))
        assertFalse(gate.shouldShowDiscountPaywall.value)
    }

    @Test
    fun expiredOneTimeOfferNeverReturns() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 0L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()
        assertTrue(gate.reportTrigger(PaywallTrigger.EXIT_INTENT))
        gate.consumeShow()

        // Past BOTH the offer deadline and the 14-day cooldown: the one-time
        // offer has lapsed and must not restart (resetOfferOnExpiry = false).
        clock.nowMillis += 20.days.inWholeMilliseconds
        assertFalse(gate.reportTrigger(PaywallTrigger.LIMIT_HIT))
        assertFalse(gate.reportTrigger(PaywallTrigger.EXIT_INTENT))

        val restartedGate = manager(source, FakeSubscriptionRepository(), clock)
        restartedGate.onAppStart()
        assertFalse(restartedGate.shouldShowDiscountPaywall.value)
    }

    @Test
    fun exitIntentFiresOnTheNextColdStartOnly() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 0L)
        val firstSession = manager(source, FakeSubscriptionRepository(), clock)

        firstSession.onAppStart()
        assertFalse(firstSession.shouldShowDiscountPaywall.value)
        assertNull(source.stored.regularPaywallDismissedSessionId)

        // User dismisses the regular paywall during session 1.
        firstSession.onRegularPaywallDismissed()
        assertEquals(1, source.stored.regularPaywallDismissedSessionId)
        assertFalse(firstSession.shouldShowDiscountPaywall.value)

        // Session 2: the promised "next app open" moment.
        clock.nowMillis += 5_000L
        val secondSession = manager(source, FakeSubscriptionRepository(), clock)
        secondSession.onAppStart()
        awaitTrue(secondSession.shouldShowDiscountPaywall)

        val expectedDeadline = 5_000L + (12.hours + 29.minutes).inWholeMilliseconds
        assertEquals(expectedDeadline, source.stored.discountOfferExpiresAt)

        // Dismissal was consumed — session 3 gets no free replay.
        assertNull(source.stored.regularPaywallDismissedSessionId)
        val thirdSession = manager(source, FakeSubscriptionRepository(), clock)
        thirdSession.onAppStart()
        assertFalse(thirdSession.shouldShowDiscountPaywall.value)
    }

    @Test
    fun anySingleTriggerSufficesWhenGatesPass() = runBlocking {
        PaywallTrigger.entries.forEach { trigger ->
            val source = FakeDataSource()
            val clock = FakeClock(nowMillis = 42L)
            val gate = manager(source, FakeSubscriptionRepository(), clock)
            gate.onAppStart()
            assertTrue(gate.reportTrigger(trigger), "$trigger should approve on clean state")
        }
    }

    @Test
    fun liveOfferRoutesEntryPointsToDiscountUntilExpiry() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 0L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()

        // No offer has ever started: premium surfaces show the regular paywall.
        assertFalse(gate.isDiscountOfferLive())

        // A trigger stamps the deadline — the offer is now live everywhere.
        gate.reportTrigger(PaywallTrigger.LIMIT_HIT)
        assertTrue(gate.isDiscountOfferLive())

        // Explicit entry points ignore the trigger cooldown while it's live:
        // t+1h is deep inside the 14-day cooldown (triggers are blocked here),
        // yet the live offer is still reachable from premium surfaces.
        clock.nowMillis += 1.hours.inWholeMilliseconds
        assertFalse(gate.reportTrigger(PaywallTrigger.LIMIT_HIT)) // cooldown blocks triggers
        assertTrue(gate.isDiscountOfferLive())                    // ...but not entry points
        gate.onDiscountPaywallShown()
        assertEquals(1.hours.inWholeMilliseconds, source.stored.discountPaywallLastShownAt)

        // The entry-point impression never moves the deadline.
        assertEquals(
            (12.hours + 29.minutes).inWholeMilliseconds,
            source.stored.discountOfferExpiresAt,
        )

        // Live right up to the deadline...
        clock.nowMillis = 12.hours.inWholeMilliseconds
        assertTrue(gate.isDiscountOfferLive())

        // ...and the moment it passes, premium surfaces fall back to regular.
        clock.nowMillis = 13.hours.inWholeMilliseconds
        assertFalse(gate.isDiscountOfferLive())
    }

    @Test
    fun entryPointNeverRoutesSubscribersToTheDiscount() = runBlocking {
        val source = FakeDataSource()
        val gate = manager(source, FakeSubscriptionRepository(initiallySubscribed = true), FakeClock())

        gate.onAppStart()
        assertFalse(gate.isDiscountOfferLive())

        // Even with a stamped deadline, a subscriber never gets the discount.
        gate.onDiscountPaywallShown()
        assertFalse(gate.isDiscountOfferLive())
    }

    @Test
    fun immediateExitIntentAfterDismissalConsumesTheArm() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 0L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()
        gate.onRegularPaywallDismissed()

        // In-session offer right after the dismissal (new onboarding behavior).
        assertTrue(gate.reportTrigger(PaywallTrigger.EXIT_INTENT))
        assertNull(source.stored.regularPaywallDismissedSessionId)
        gate.consumeShow()

        // The arm was consumed by the immediate impression: no replay on the
        // next cold start.
        clock.nowMillis += 5_000L
        val secondSession = manager(source, FakeSubscriptionRepository(), clock)
        secondSession.onAppStart()
        assertFalse(secondSession.shouldShowDiscountPaywall.value)
    }

    @Test
    fun stateSurvivesRestart() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 100L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()
        gate.reportTrigger(PaywallTrigger.LIMIT_HIT)

        val expectedLastShown = 100L
        val expectedDeadline = 100L + (12.hours + 29.minutes).inWholeMilliseconds
        val restartedGate = manager(source, FakeSubscriptionRepository(), clock)
        restartedGate.onAppStart()
        assertEquals(expectedLastShown, source.stored.discountPaywallLastShownAt)
        assertEquals(expectedDeadline, source.stored.discountOfferExpiresAt)
        assertEquals(2, source.stored.launchSessionId)
    }

    @Test
    fun winBackAutoFiresWhenEntitlementFlipsToUnsubscribed() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 0L)
        val repo = FakeSubscriptionRepository(initiallySubscribed = true)
        val gate = manager(source, repo, clock)

        gate.onAppStart()

        // While subscribed, every gate fails and the sticky flag persists.
        withTimeout(3_000) { while (!source.stored.isSubscribed) delay(5) }
        assertFalse(gate.reportTrigger(PaywallTrigger.WIN_BACK))
        assertFalse(gate.shouldShowDiscountPaywall.value)

        // Trial expired / cancelled: the live entitlement flips to false. The
        // sticky flag un-sticks and WIN_BACK fires without any caller action.
        clock.nowMillis += 1_000L
        repo.setSubscribed(false)
        awaitTrue(gate.shouldShowDiscountPaywall)

        assertFalse(source.stored.isSubscribed)
        assertEquals(1_000L, source.stored.discountPaywallLastShownAt)
        assertEquals(1_000L + (12.hours + 29.minutes).inWholeMilliseconds, source.stored.discountOfferExpiresAt)

        // The impression is a normal one: cooldown now applies.
        gate.consumeShow()
        clock.nowMillis += 2.days.inWholeMilliseconds
        assertFalse(gate.reportTrigger(PaywallTrigger.LIMIT_HIT))
    }

    @Test
    fun convenienceWrappersRouteThroughTheSameGates() = runBlocking {
        val source = FakeDataSource()
        val clock = FakeClock(nowMillis = 7L)
        val gate = manager(source, FakeSubscriptionRepository(), clock)

        gate.onAppStart()
        assertTrue(gate.onPremiumFeatureBlocked())
        assertEquals(7L, source.stored.discountPaywallLastShownAt)
        assertEquals(7L + (12.hours + 29.minutes).inWholeMilliseconds, source.stored.discountOfferExpiresAt)

        // The impression was delivered; clear the approval flag like navigation would.
        gate.consumeShow()

        // The wrapper's impression starts the cooldown like any other trigger.
        assertFalse(gate.onCheckoutAbandoned())
        assertFalse(gate.shouldShowDiscountPaywall.value)
    }

    @Test
    fun cancellationBeforeAppStartDoesNotFireWinBack() = runBlocking {
        val source = FakeDataSource()
        val repo = FakeSubscriptionRepository(initiallySubscribed = true)
        val gate = manager(source, repo, clock = FakeClock(nowMillis = 0L))

        // Flip before onAppStart: no started engine, no auto-impression.
        repo.setSubscribed(false)
        gate.onAppStart()
        assertFalse(gate.shouldShowDiscountPaywall.value)
        assertNull(source.stored.discountPaywallLastShownAt)
    }
}
