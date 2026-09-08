package app.usefoster.shared.version

import platform.Foundation.NSBundle

actual fun currentAppVersion(): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "Unknown"