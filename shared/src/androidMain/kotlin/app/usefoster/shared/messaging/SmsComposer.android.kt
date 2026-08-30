package app.usefoster.shared.messaging

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSmsComposer(
    onUnavailable: () -> Unit,
): (phoneNumber: String, body: String) -> Unit {
    val context = LocalContext.current
    return remember(context, onUnavailable) {
        { phoneNumber: String, body: String ->
            try {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$phoneNumber")
                        putExtra("sms_body", body)
                    },
                )
            } catch (_: ActivityNotFoundException) {
                onUnavailable()
            }
        }
    }
}
