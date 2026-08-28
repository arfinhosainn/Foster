package app.usenekko.shared.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSMutableCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication

@Composable
actual fun rememberSmsComposer(
    onUnavailable: () -> Unit,
): (phoneNumber: String, body: String) -> Unit {
    return remember(onUnavailable) {
        { phoneNumber: String, body: String ->
            val url = NSURL.URLWithString(
                "sms:$phoneNumber&body=${body.percentEncodedForSms()}",
            )
            if (url == null) {
                onUnavailable()
            } else {
                UIApplication.sharedApplication.openURL(
                    url = url,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = { opened -> if (!opened) onUnavailable() },
                )
            }
        }
    }
}

/** Percent-encodes everything outside the RFC 3986 unreserved set. */
private fun String.percentEncodedForSms(): String {
    val allowed = NSMutableCharacterSet.alphanumericCharacterSet()
    allowed.addCharactersInString("-._~")
    return (this as NSString)
        .stringByAddingPercentEncodingWithAllowedCharacters(allowed)
        .orEmpty()
}
