package app.usenekko.theme

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

fun resolveDarkTheme(mode: AppThemeMode, systemDark: Boolean): Boolean = when (mode) {
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
    AppThemeMode.SYSTEM -> systemDark
}