package app.usenekko.navigation

sealed class Screen {
    data object Welcome : Screen()
    data object Phone : Screen()
    data class CodeVerification(val phoneNumber: String) : Screen()
    data object Name : Screen()
    data object Contact : Screen()
}
