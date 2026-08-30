package app.usefoster.shared.paywall

/**
 * Durable storage for [PaywallGateState]. Platform implementations live in
 * androidMain (DataStore) and iosMain (NSUserDefaults) — same pattern as
 * ThemePreferenceDataSource.
 */
interface PaywallGateDataSource {
    suspend fun getState(): PaywallGateState

    suspend fun setState(state: PaywallGateState)
}