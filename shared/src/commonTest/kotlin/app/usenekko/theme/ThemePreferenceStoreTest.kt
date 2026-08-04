package app.usenekko.theme

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreferenceStoreTest {

    private class FakeDataSource(initial: AppThemeMode) : ThemePreferenceDataSource {
        var stored: AppThemeMode = initial
        var loaded: Boolean = false
        override suspend fun getMode(): AppThemeMode {
            loaded = true
            return stored
        }

        override suspend fun setMode(mode: AppThemeMode) {
            stored = mode
        }
    }

    private suspend fun awaitStored(dataSource: FakeDataSource, expected: AppThemeMode) {
        withTimeout(3_000) {
            while (dataSource.stored != expected) {
                delay(5)
            }
        }
    }

    private suspend fun awaitMode(store: ThemePreferenceStore, expected: AppThemeMode) {
        withTimeout(3_000) {
            while (store.mode.value != expected) {
                delay(5)
            }
        }
    }

    private suspend fun awaitInit(dataSource: FakeDataSource) {
        withTimeout(3_000) {
            while (!dataSource.loaded) {
                delay(5)
            }
        }
    }

    @Test
    fun initLoadsPersistedMode() = runBlocking {
        val dataSource = FakeDataSource(AppThemeMode.DARK)
        val store = ThemePreferenceStore(dataSource)

        awaitMode(store, AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, store.mode.value)
    }

    @Test
    fun initDefaultsToSystemWhenNothingPersisted() = runBlocking {
        val store = ThemePreferenceStore(FakeDataSource(AppThemeMode.SYSTEM))

        awaitMode(store, AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, store.mode.value)
    }

    @Test
    fun setModeUpdatesFlowImmediatelyAndPersists() = runBlocking {
        val dataSource = FakeDataSource(AppThemeMode.SYSTEM)
        val store = ThemePreferenceStore(dataSource)

        awaitInit(dataSource)

        store.setMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, store.mode.value)

        awaitStored(dataSource, AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, dataSource.stored)

        store.setMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, store.mode.value)
        awaitStored(dataSource, AppThemeMode.DARK)
    }
}

class ResolveDarkThemeTest {

    @Test
    fun darkModeAlwaysDark() {
        assertEquals(true, resolveDarkTheme(AppThemeMode.DARK, systemDark = true))
        assertEquals(true, resolveDarkTheme(AppThemeMode.DARK, systemDark = false))
    }

    @Test
    fun lightModeAlwaysLight() {
        assertEquals(false, resolveDarkTheme(AppThemeMode.LIGHT, systemDark = true))
        assertEquals(false, resolveDarkTheme(AppThemeMode.LIGHT, systemDark = false))
    }

    @Test
    fun systemModeFollowsSystem() {
        assertEquals(true, resolveDarkTheme(AppThemeMode.SYSTEM, systemDark = true))
        assertEquals(false, resolveDarkTheme(AppThemeMode.SYSTEM, systemDark = false))
    }
}