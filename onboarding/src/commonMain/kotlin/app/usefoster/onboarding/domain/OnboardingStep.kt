package app.usefoster.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStep(val index: Int) {
    Welcome(0),
    Name(1),
    Contact(2),
    Group(3),
    DayReminder(4),
    TimeReminder(5),
    CustomReminder(6),
    AddNote(7),
    Notification(8),
    Complete(9),
}
