package app.usefoster.shared.messaging

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareComposer(
    onUnavailable: () -> Unit,
): (text: String) -> Unit {
    val context = LocalContext.current
    return remember(context, onUnavailable) {
        { text: String ->
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, null),
                )
            } catch (_: ActivityNotFoundException) {
                onUnavailable()
            }
        }
    }
}