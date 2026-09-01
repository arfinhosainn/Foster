package app.usefoster.onboarding.name

/**
 * Name validation for the "What should we call you?" onboarding step:
 * non-blank and between 4 and 50 characters.
 */
class ValidateName() {
    fun validate(name: String): Boolean {
        if (name.isBlank()) return false
        return name.length in 4..50             // 4-50 chars
    }
}
