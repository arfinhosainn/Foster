package app.usenekko.onboarding.email

sealed interface EmailAction {
    data class EmailChanged(val email: String) : EmailAction
    data object ContinueClicked : EmailAction
    data object BackClicked : EmailAction
    data object SkipClicked : EmailAction
}
