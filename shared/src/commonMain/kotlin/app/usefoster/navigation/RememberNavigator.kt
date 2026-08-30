package app.usefoster.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Composable
fun rememberNavigator(startDestination: Screen): Navigator {
    return rememberSaveable(
        startDestination,
        saver = NavigatorSaver
    ) {
        Navigator(startDestination)
    }
}

private val NavigationJson = Json {
    ignoreUnknownKeys = true
}

private val ScreenStackSerializer = ListSerializer(Screen.serializer())

private val NavigatorSaver = listSaver<Navigator, String>(
    save = { navigator ->
        listOf(NavigationJson.encodeToString(ScreenStackSerializer, navigator.backStack.toList()))
    },
    restore = { restored ->
        val screens = restored.firstOrNull()
            ?.let { NavigationJson.decodeFromString(ScreenStackSerializer, it) }
            .orEmpty()
        Navigator(screens.toSnapshotStateList())
    }
)

private fun List<Screen>.toSnapshotStateList(): SnapshotStateList<Screen> {
    return if (isEmpty()) {
        mutableListOf(Screen.Welcome)
    } else {
        this
    }.toMutableStateList()
}
