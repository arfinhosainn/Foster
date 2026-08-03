package app.usenekko.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Welcome : Screen()

    @Serializable
    data object Name : Screen()

    @Serializable
    data object Group : Screen()

    @Serializable
    data object Reminder : Screen()

    @Serializable
    data object TimeReminder : Screen()

    @Serializable
    data object CustomReminder : Screen()

    @Serializable
    data object AddNote : Screen()

    @Serializable
    data object Notification : Screen()

    @Serializable
    data object Contact : Screen()

    @Serializable
    data object Home : Screen()

    @Serializable
    data class ContactProfile(val contactId: String) : Screen()
}
