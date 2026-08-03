package app.usenekko.home.domain

data class Note(
    val id: String,
    val contactId: String,
    val title: String,
    val body: String,
    val createdAt: String,
)
