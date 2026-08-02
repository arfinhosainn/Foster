package app.usenekko.home.addcontact

sealed interface AddContactEvent {
    data object Saved : AddContactEvent
}
