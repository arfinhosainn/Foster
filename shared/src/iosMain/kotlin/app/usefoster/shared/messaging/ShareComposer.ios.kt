package app.usefoster.shared.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.usefoster.shared.contacts.currentViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareComposer(
    onUnavailable: () -> Unit,
): (text: String) -> Unit {
    return remember(onUnavailable) {
        { text: String ->
            val presenter = currentViewController()
            if (presenter == null) {
                onUnavailable()
            } else {
                val activityController = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null,
                )
                activityController.popoverPresentationController?.let { popover ->
                    // iPad requires an anchored popover, otherwise presentation crashes.
                    popover.sourceView = presenter.view
                    popover.sourceRect = presenter.view.bounds
                }
                presenter.presentViewController(
                    activityController,
                    animated = true,
                    completion = null,
                )
            }
        }
    }
}