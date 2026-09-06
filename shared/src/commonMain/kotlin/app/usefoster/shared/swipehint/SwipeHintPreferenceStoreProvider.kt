package app.usefoster.shared.swipehint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSwipeHintStore = staticCompositionLocalOf<SwipeHintPreferenceStore?> { null }

@Composable
fun SwipeHintPreferenceStoreProvider(content: @Composable () -> Unit) {
    val store = rememberSwipeHintStore()
    CompositionLocalProvider(LocalSwipeHintStore provides store) {
        content()
    }
}

@Composable
expect fun rememberSwipeHintStore(): SwipeHintPreferenceStore