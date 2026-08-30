package app.usefoster.onboarding.group

/**
 * Represents a single group in the onboarding group-selection screen.
 *
 * @param id       Unique identifier for the group.
 * @param name     Display name (e.g. "Family", "Friends").
 * @param memberAvatarIds List of selected avatar ids used for the overlapping circles.
 */
data class Group(
    val id: String,
    val name: String,
    val memberAvatarIds: List<String> = emptyList(),
)
