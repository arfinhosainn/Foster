package app.usenekko.shared.messaging

import androidx.compose.runtime.Composable

/**
 * Opens the platform SMS/Messages app with [phoneNumber] as the recipient and
 * [body] pre-filled in the compose field. The user can edit before sending.
 * Invokes [onUnavailable] when no SMS app can handle the request.
 */
@Composable
expect fun rememberSmsComposer(
    onUnavailable: () -> Unit = {},
): (phoneNumber: String, body: String) -> Unit
