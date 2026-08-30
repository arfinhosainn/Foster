package app.usefoster.theme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemePreferenceStore(
    private val dataSource: ThemePreferenceDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _mode = MutableStateFlow(AppThemeMode.SYSTEM)
    val mode: StateFlow<AppThemeMode> = _mode.asStateFlow()

    init {
        scope.launch {
            _mode.value = dataSource.getMode()
        }
    }

    fun setMode(mode: AppThemeMode) {
        _mode.value = mode
        scope.launch {
            dataSource.setMode(mode)
        }
    }
}