package app.usenekko.onboarding.domain

import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStep(val index: Int) {
    Welcome(0),
    Email(1),
    EmailVerification(2),
    Name(3),
    Contact(4),
    Group(5),
    DayReminder(6),
    TimeReminder(7),
    CustomReminder(8),
    AddNote(9),
    Notification(10),
    Complete(11),
}
