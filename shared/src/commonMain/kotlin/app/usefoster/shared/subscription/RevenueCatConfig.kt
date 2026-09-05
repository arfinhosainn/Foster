package app.usefoster.shared.subscription

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure

/**
 * Platform-specific RevenueCat **public** SDK key.
 *
 * - Android: Google Play public key (find it in the RevenueCat dashboard under
 *   Project Settings → API Keys → App-specific keys, prefixed `goog_`).
 * - iOS: Apple public key (same dashboard, prefixed `appl_`).
 *
 * These are PUBLIC SDK keys — safe to ship client-side (unlike Supabase
 * service-role keys). Replace the placeholder values in the platform `actual`
 * files with your real keys before testing.
 */
expect val revenueCatApiKey: String

/**
 * Configure RevenueCat once, early in the app lifecycle. Call this from the
 * Android Activity's [onCreate] and the iOS [ComposeUIViewController] entry point.
 *
 * Safe to call multiple times — guards on [Purchases.isConfigured].
 */
fun initRevenueCat() {
    if (Purchases.isConfigured) return
    // Unconfigured (blank key — e.g. Secrets not set up yet): skip so callers
    // get NotConfigured instead of a backend-auth error on every cold start.
    if (revenueCatApiKey.isBlank()) return
    Purchases.configure(apiKey = revenueCatApiKey)
}
