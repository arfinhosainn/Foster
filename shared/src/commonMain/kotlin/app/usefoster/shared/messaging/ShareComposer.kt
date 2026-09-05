package app.usefoster.shared.messaging

import androidx.compose.runtime.Composable

/**
 * Opens the platform share sheet (Android system Sharesheet /
 * iOS UIActivityViewController) with [text] pre-filled. The user then picks a
 * target app (Messages, WhatsApp, Telegram, Signal, …) and confirms sending
 * there — nothing is auto-sent, and the destination app's own
 * contact/conversation picker handles recipient selection.
 *
 * Invokes [onUnavailable] when no installed app can handle a text share.
 */
@Composable
expect fun rememberShareComposer(
    onUnavailable: () -> Unit = {},
): (text: String) -> Unit