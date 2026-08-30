package app.usefoster.onboarding.name

sealed interface NameAction {
    data class OnFirstNameChange(val value: String) : NameAction
    data class OnLastNameChange(val value: String) : NameAction
    data object OnContinueClick : NameAction
}
