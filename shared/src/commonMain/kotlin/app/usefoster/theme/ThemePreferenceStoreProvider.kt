package app.usefoster.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalThemeStore = staticCompositionLocalOf<ThemePreferenceStore?> { null }

@Composable
fun ThemePreferenceStoreProvider(content: @Composable () -> Unit) {
    val store = rememberThemePreferenceStore()
    CompositionLocalProvider(LocalThemeStore provides store) {
        content()
    }
}

@Composable
expect fun rememberThemePreferenceStore(): ThemePreferenceStore