package app.usenekko.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

private class DataStoreThemePreferenceDataSource(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferenceDataSource {

    private object Keys {
        val Mode = stringPreferencesKey("app_theme_mode")
    }

    override suspend fun getMode(): AppThemeMode {
        val prefs = dataStore.data.first()
        return prefs[Keys.Mode]?.let { name ->
            AppThemeMode.entries.firstOrNull { it.name == name }
        } ?: AppThemeMode.SYSTEM
    }

    override suspend fun setMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.Mode] = mode.name
        }
    }
}

@Composable
actual fun rememberThemePreferenceStore(): ThemePreferenceStore {
    val context = LocalContext.current
    return remember {
        ThemePreferenceStore(DataStoreThemePreferenceDataSource(context.themeDataStore))
    }
}