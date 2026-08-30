package app.usefoster.theme

interface ThemePreferenceDataSource {
    suspend fun getMode(): AppThemeMode
    suspend fun setMode(mode: AppThemeMode)
}