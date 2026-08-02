package app.usenekko.home.domain

data class Contact(
    val id: String,
    val name: String,
    val avatarColor: String?,
    val checkInFrequency: String,
    val reminderTime: String?,
    val nextCheckInDate: String?,
    val lastCheckInDate: String?,
    val streakCount: Int,
)
