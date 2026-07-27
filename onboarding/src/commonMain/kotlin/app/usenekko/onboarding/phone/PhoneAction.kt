package app.usenekko.onboarding.phone

sealed interface PhoneAction {
    data class PhoneNumberChanged(val phoneNumber: String) : PhoneAction
    data object ContinueClicked : PhoneAction
    data object BackClicked : PhoneAction
    data object SkipClicked : PhoneAction
}
