package app.usenekko.onboarding.contact

sealed interface ContactEvent {
    data object NavigateToNext : ContactEvent
    data object NavigateBack : ContactEvent
    data object NavigateSkip : ContactEvent
    data object RequestContactPermission : ContactEvent
    data class ContactImported(val name: String) : ContactEvent
    data object ContactPermissionDenied : ContactEvent
}
