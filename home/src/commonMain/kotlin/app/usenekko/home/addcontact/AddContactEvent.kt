package app.usenekko.home.addcontact

sealed interface AddContactEvent {
    data object Saved : AddContactEvent
    data class ShowPaywall(
        val reason: app.usenekko.shared.subscription.BlockReason,
        /** True when the gate engine approved a discount impression for this trigger. */
        val showDiscount: Boolean = false,
    ) : AddContactEvent
}
