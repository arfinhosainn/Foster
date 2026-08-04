package app.usenekko.theme

interface ThemePreferenceDataSource {
    suspend fun getMode(): AppThemeMode
    suspend fun setMode(mode: AppThemeMode)
}