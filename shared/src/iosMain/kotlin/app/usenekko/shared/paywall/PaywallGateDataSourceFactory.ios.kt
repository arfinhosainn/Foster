package app.usenekko.shared.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private class NSUserDefaultsPaywallGateDataSource : PaywallGateDataSource {

    private val defaults = NSUserDefaults.standardUserDefaults

    private object Keys {
        const val IsSubscribed = "paywall_gate_is_subscribed"
        const val LaunchSessionId = "paywall_gate_launch_session_id"
        const val RegularDismissedSessionId = "paywall_gate_regular_dismissed_session"
        const val DiscountLastShownAt = "paywall_gate_discount_last_shown_at"
        const val OfferExpiresAt = "paywall_gate_offer_expires_at"
    }

    override suspend fun getState(): PaywallGateState {
        return PaywallGateState(
            isSubscribed = defaults.boolForKey(Keys.IsSubscribed),
            launchSessionId = defaults.integerForKey(Keys.LaunchSessionId).toInt(),
            regularPaywallDismissedSessionId = nullableInt(Keys.RegularDismissedSessionId),
            discountPaywallLastShownAt = nullableLong(Keys.DiscountLastShownAt),
            discountOfferExpiresAt = nullableLong(Keys.OfferExpiresAt),
        )
    }

    override suspend fun setState(state: PaywallGateState) {
        defaults.setBool(state.isSubscribed, forKey = Keys.IsSubscribed)
        defaults.setInteger(state.launchSessionId.toLong(), forKey = Keys.LaunchSessionId)
        setNullableInt(Keys.RegularDismissedSessionId, state.regularPaywallDismissedSessionId)
        setNullableLong(Keys.DiscountLastShownAt, state.discountPaywallLastShownAt)
        setNullableLong(Keys.OfferExpiresAt, state.discountOfferExpiresAt)
        defaults.synchronize()
    }

    private fun nullableInt(key: String): Int? =
        if (defaults.objectForKey(key) == null) null else defaults.integerForKey(key).toInt()

    private fun nullableLong(key: String): Long? =
        if (defaults.objectForKey(key) == null) null else defaults.doubleForKey(key).toLong()

    private fun setNullableInt(key: String, value: Int?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setInteger(value.toLong(), forKey = key)
        }
    }

    private fun setNullableLong(key: String, value: Long?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setDouble(value.toDouble(), forKey = key)
        }
    }
}

@Composable
actual fun rememberPaywallGateDataSource(): PaywallGateDataSource {
    return remember { NSUserDefaultsPaywallGateDataSource() }
}
