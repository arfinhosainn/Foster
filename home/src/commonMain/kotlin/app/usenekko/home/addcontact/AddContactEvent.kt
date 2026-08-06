package app.usenekko.home.addcontact

sealed interface AddContactEvent {
    data object Saved : AddContactEvent
    data class ShowPaywall(val reason: app.usenekko.shared.subscription.BlockReason) : AddContactEvent
}
