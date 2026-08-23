package app.usenekko.shared.paywall

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Durable paywall-gate state. Everything here survives app restarts (DataStore
 * on Android, NSUserDefaults on iOS).
 *
 * @property isSubscribed            Mirror of the RevenueCat entitlement so the
 *                                   gates also work offline. Sticky by design:
 *                                   once the user has ever subscribed, neither
 *                                   paywall is shown again.
 * @property launchSessionId         Monotonically increasing cold-start counter,
 *                                   used by the EXIT_INTENT trigger to detect
 *                                   "next app open after dismissal".
 * @property regularPaywallDismissedSessionId  Session in which the regular
 *                                   paywall was last dismissed (null = never).
 * @property discountPaywallLastShownAt  Epoch millis of the last discount show.
 * @property discountOfferExpiresAt  Epoch millis deadline for the countdown;
 *                                   stamped the FIRST time the offer is shown.
 */
data class PaywallGateState(
    val isSubscribed: Boolean = false,
    val launchSessionId: Int = 0,
    val regularPaywallDismissedSessionId: Int? = null,
    val discountPaywallLastShownAt: Long? = null,
    val discountOfferExpiresAt: Long? = null,
)

/**
 * Tunables for the discount-paywall gate. Change values here to retune the
 * campaign without touching logic.
 */
object PaywallGateConfig {
    /** Minimum time between two discount-paywall impressions. */
    val discountCooldown: Duration = 14.days

    /** Live-offer window; the on-screen countdown runs against this deadline. */
    val discountOfferDuration: Duration = 12.hours + 29.minutes

    /** When true, an expired offer restarts (fresh deadline) on next trigger. */
    const val resetOfferOnExpiry: Boolean = false
}