package app.usefoster.shared.swipehint

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.swipeHintDataStore by preferencesDataStore(name = "swipe_hint_preferences")

private class DataStoreSwipeHintPreferenceDataSource(
    private val dataStore: DataStore<Preferences>,
) : SwipeHintPreferenceDataSource {

    private object Keys {
        val SeenHint = booleanPreferencesKey("has_seen_hint")
        val SwipedBook = booleanPreferencesKey("has_swiped_home_book")
    }

    override suspend fun hasSeenHint(): Boolean =
        dataStore.data.first()[Keys.SeenHint] ?: false

    override suspend fun setHasSeenHint(seen: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SeenHint] = seen }
    }

    override suspend fun hasSwipedBook(): Boolean =
        dataStore.data.first()[Keys.SwipedBook] ?: false

    override suspend fun setHasSwipedBook(swiped: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SwipedBook] = swiped }
    }
}

@Composable
actual fun rememberSwipeHintStore(): SwipeHintPreferenceStore {
    val context = LocalContext.current
    return remember {
        SwipeHintPreferenceStore(DataStoreSwipeHintPreferenceDataSource(context.swipeHintDataStore))
    }
}