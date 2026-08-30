package app.usefoster.onboarding.name

data class NameState(
    val firstName: String = "",
    val lastName: String = "",
) {
    val isFormValid: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
}
