package app.usefoster.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private class NSUserDefaultsThemePreferenceDataSource : ThemePreferenceDataSource {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val key = "app_theme_mode"

    override suspend fun getMode(): AppThemeMode {
        return defaults.stringForKey(key)?.let { name ->
            AppThemeMode.entries.firstOrNull { it.name == name }
        } ?: AppThemeMode.SYSTEM
    }

    override suspend fun setMode(mode: AppThemeMode) {
        defaults.setObject(mode.name, forKey = key)
        defaults.synchronize()
    }
}

@Composable
actual fun rememberThemePreferenceStore(): ThemePreferenceStore {
    return remember {
        ThemePreferenceStore(NSUserDefaultsThemePreferenceDataSource())
    }
}