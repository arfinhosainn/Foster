package app.usenekko.shared.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.usenekko.shared.subscription.SubscriptionRepository
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Why the discount paywall wants to appear. Any single trigger is enough —
 * the global gates still have to pass.
 */
enum class PaywallTrigger {
    /** Next app open after the regular paywall was dismissed. */
    EXIT_INTENT,

    /** User just experienced core value (first check-in, first brainstorm...). */
    AHA_MOMENT,

    /** Free contact limit blocked an action. */
    LIMIT_HIT,

    /** User opened the native purchase sheet on a paywall and backed out. */
    ABANDONED_CHECKOUT,

    /** Lapsed / inactive user returned. */
    WIN_BACK,
}

/**
 * The discount-paywall decision engine. Pure decision logic + persistence —
 * knows nothing about screens or navigation; UI observes
 * [shouldShowDiscountPaywall] and routes accordingly.
 *
 * Global gates (ALL required to show):
 *  - never subscribed (sticky: once ever subscribed → never again)
 *  - outside [PaywallGateConfig.discountCooldown] since last impression
 *  - offer still live ([PaywallGateState.discountOfferExpiresAt] in the future,
 *    or no deadline yet; with resetOfferOnExpiry=false an expired offer never returns)
 */
class PaywallGateManager(
    private val dataSource: PaywallGateDataSource,
    private val subscriptionRepository: SubscriptionRepository,
    private val clock: Clock = Clock.System,
    backgroundDispatcher: CoroutineContext = Dispatchers.Default,
) {

    private val scope = CoroutineScope(SupervisorJob() + backgroundDispatcher)

    private val _shouldShowDiscountPaywall = MutableStateFlow(false)

    /** True while the engine has approved an impression that wasn't delivered yet. */
    val shouldShowDiscountPaywall: StateFlow<Boolean> = _shouldShowDiscountPaywall.asStateFlow()

    private val _offerExpiresAtMillis = MutableStateFlow<Long?>(null)

    /**
     * Real countdown deadline for the current offer (null until the first show
     * stamps it). The screen ticks against this so remounts never reset it.
     */
    val offerExpiresAtMillis: StateFlow<Long?> = _offerExpiresAtMillis.asStateFlow()

    @Volatile
    private var cachedState: PaywallGateState = PaywallGateState()

    /** Last value seen from the live entitlement flow (null until first emission). */
    @Volatile
    private var lastSeenSubscribed: Boolean? = null

    private var started = false

    /**
     * Called exactly once per cold start (from PaywallGateManagerProvider).
     * Loads persisted state, mirrors live entitlement into it, bumps the
     * launch-session counter and evaluates the EXIT_INTENT trigger.
     */
    suspend fun onAppStart() {
        val loaded = dataSource.getState()
        val now = nowMillis()
        val newSessionId = loaded.launchSessionId + 1

        // Entitlement truth lives in RevenueCat; mirror it so gates also work
        // offline and stay sticky once the user has ever subscribed.
        var state = loaded.copy(launchSessionId = newSessionId)
        if (subscriptionRepository.isSubscribed.value && !state.isSubscribed) {
            state = state.copy(isSubscribed = true)
        }

        cachedState = state
        persist(state)
        _offerExpiresAtMillis.value = state.discountOfferExpiresAt

        started = true

        // EXIT_INTENT: the regular paywall was dismissed during an earlier
        // session — this open is the "next app open" moment.
        val dismissedInEarlierSession =
            state.regularPaywallDismissedSessionId != null &&
                state.regularPaywallDismissedSessionId < newSessionId
        if (dismissedInEarlierSession) {
            evaluate(PaywallTrigger.EXIT_INTENT, now)
        }
    }

    /** Arms EXIT_INTENT for the next cold start. Called when the regular paywall closes. */
    fun onRegularPaywallDismissed() {
        scope.launch {
            updateAndPersist { it.copy(regularPaywallDismissedSessionId = it.launchSessionId) }
        }
    }

    /**
     * Reports a trigger moment. Returns true when the impression was approved
     * (all gates passed) — the caller may navigate immediately; observers of
     * [shouldShowDiscountPaywall] see true as well.
     */
    suspend fun reportTrigger(trigger: PaywallTrigger): Boolean {
        if (!started) return false
        return evaluate(trigger, nowMillis())
    }

    /** Marks the approved impression as delivered (navigation actually happened). */
    fun consumeShow() {
        _shouldShowDiscountPaywall.value = false
    }

    /**
     * True while a live discounted offer exists for this user: it has been
     * started (a deadline was stamped), has NOT expired yet, and the user is
     * not subscribed. Used by explicit premium entry points (home crown
     * button, settings upgrade card, ...) to prefer the discount paywall;
     * deliberately ignores the trigger cooldown — while the countdown runs,
     * every premium surface leads to the deal.
     */
    fun isDiscountOfferLive(): Boolean {
        if (!started) return false
        val state = cachedState
        if (state.isSubscribed) return false
        val expiresAt = state.discountOfferExpiresAt ?: return false
        return expiresAt > nowMillis()
    }

    /**
     * Records a discount-paywall impression opened from an explicit entry
     * point. Refreshes the cooldown anchor so background triggers respect
     * this impression; never touches the offer deadline.
     */
    suspend fun onDiscountPaywallShown() {
        if (!started) return
        updateAndPersist { it.copy(discountPaywallLastShownAt = nowMillis()) }
    }

    /** User completed a core value action (e.g. a check-in). */
    suspend fun onCoreValueActionCompleted(): Boolean =
        reportTrigger(PaywallTrigger.AHA_MOMENT)

    /** User attempted a premium/gated feature and was blocked. */
    suspend fun onPremiumFeatureBlocked(): Boolean =
        reportTrigger(PaywallTrigger.LIMIT_HIT)

    /** User opened the purchase sheet and closed it without buying. */
    suspend fun onCheckoutAbandoned(): Boolean =
        reportTrigger(PaywallTrigger.ABANDONED_CHECKOUT)

    /** Trial expired or the subscription was cancelled. */
    suspend fun onTrialExpiredOrCancelled(): Boolean =
        reportTrigger(PaywallTrigger.WIN_BACK)

    private suspend fun evaluate(trigger: PaywallTrigger, now: Long): Boolean {
        val state = cachedState
        if (!gatesPass(state, now)) return false

        var updated = state.copy(
            discountPaywallLastShownAt = now,
            regularPaywallDismissedSessionId =
                if (trigger == PaywallTrigger.EXIT_INTENT) null
                else state.regularPaywallDismissedSessionId,
        )

        // Stamp the real countdown deadline the FIRST time the offer shows;
        // later impressions keep the original deadline (no reset on remount).
        val expired = updated.discountOfferExpiresAt != null && updated.discountOfferExpiresAt <= now
        val needsDeadline = when {
            updated.discountOfferExpiresAt == null -> true
            expired && PaywallGateConfig.resetOfferOnExpiry -> true
            else -> false
        }
        if (needsDeadline) {
            updated = updated.copy(discountOfferExpiresAt = now + PaywallGateConfig.discountOfferDuration.inWholeMilliseconds)
        } else if (expired) {
            // Offer expired and must not restart — gates should have caught it.
            return false
        }

        cachedState = updated
        persist(updated)
        _offerExpiresAtMillis.value = updated.discountOfferExpiresAt
        _shouldShowDiscountPaywall.value = true
        return true
    }

    private fun gatesPass(state: PaywallGateState, now: Long): Boolean {
        if (state.isSubscribed) return false

        val lastShown = state.discountPaywallLastShownAt
        if (lastShown != null && now - lastShown < PaywallGateConfig.discountCooldown.inWholeMilliseconds) {
            return false
        }

        val expiresAt = state.discountOfferExpiresAt
        if (expiresAt != null && expiresAt <= now && !PaywallGateConfig.resetOfferOnExpiry) {
            return false
        }
        return true
    }

    /** Serializes durable writes so rapid triggers can never reorder state on disk. */
    private val writeMutex = Mutex()

    private suspend fun updateAndPersist(transform: (PaywallGateState) -> PaywallGateState) {
        writeMutex.withLock {
            val updated = transform(cachedState)
            cachedState = updated
            persistSafely(updated)
        }
    }

    private suspend fun persist(state: PaywallGateState) {
        writeMutex.withLock {
            cachedState = state
            persistSafely(state)
        }
    }

    private suspend fun persistSafely(state: PaywallGateState) {
        try {
            dataSource.setState(state)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
        }
    }

    init {
        // Purchase kills both paywalls forever: any transition to subscribed is
        // mirrored into the persisted sticky flag, which fails every gate.
        // The reverse flip (subscribed -> not subscribed, e.g. a trial expiring
        // or the user cancelling) re-opens the gates for churned users and
        // fires the WIN_BACK trigger automatically.
        scope.launch {
            subscriptionRepository.isSubscribed.collect { subscribed ->
                val previous = lastSeenSubscribed
                lastSeenSubscribed = subscribed
                if (subscribed) {
                    _shouldShowDiscountPaywall.value = false
                    updateAndPersist { it.copy(isSubscribed = true) }
                } else if (previous == true && started) {
                    // Un-stick the persisted flag so win-back offers can reach
                    // users whose entitlement lapsed, then report the trigger.
                    updateAndPersist { it.copy(isSubscribed = false) }
                    evaluate(PaywallTrigger.WIN_BACK, nowMillis())
                }
            }
        }
    }

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()
}

@Composable
fun rememberPaywallGateManager(
    subscriptionRepository: SubscriptionRepository,
): PaywallGateManager {
    val dataSource = rememberPaywallGateDataSource()
    return remember(dataSource, subscriptionRepository) {
        PaywallGateManager(dataSource, subscriptionRepository)
    }
}
