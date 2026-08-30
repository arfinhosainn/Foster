package app.usefoster.shared.subscription

import foster.shared.generated.resources.Res
import foster.shared.generated.resources.error_subscription_network
import foster.shared.generated.resources.error_subscription_unavailable
import foster.shared.generated.resources.error_subscription_unknown
import org.jetbrains.compose.resources.StringResource

fun SubscriptionError.toUserMessage(): String = when (this) {
    SubscriptionError.NotConfigured -> "Subscriptions are temporarily unavailable. Please try again later."
    SubscriptionError.Network -> "Check your connection and try again."
    is SubscriptionError.Unknown -> "Something went wrong. Please try again."
}

fun SubscriptionError.toUserMessageResource(): StringResource = when (this) {
    SubscriptionError.NotConfigured -> Res.string.error_subscription_unavailable
    SubscriptionError.Network -> Res.string.error_subscription_network
    is SubscriptionError.Unknown -> Res.string.error_subscription_unknown
}