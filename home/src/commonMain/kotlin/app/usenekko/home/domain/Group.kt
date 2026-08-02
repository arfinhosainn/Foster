package app.usenekko.home.domain

data class Group(
    val id: String,
    val name: String,
    val color: String? = null,
)

data class GroupMembership(
    val contactId: String,
    val groupId: String,
)
