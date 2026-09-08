package app.usefoster.shared.version

import android.content.Context

private var versionName: String = "Unknown"

fun configureAppVersion(context: Context) {
    versionName = context.applicationContext.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        ?.takeIf { it.isNotBlank() }
        ?: "Unknown"
}

actual fun currentAppVersion(): String = versionName