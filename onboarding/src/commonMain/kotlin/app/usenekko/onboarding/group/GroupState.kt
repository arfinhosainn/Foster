package app.usenekko.onboarding.group

import androidx.compose.ui.graphics.Color

/**
 * Represents a single group in the onboarding group-selection screen.
 *
 * @param id       Unique identifier for the group.
 * @param name     Display name (e.g. "Family", "Friends").
 * @param members  List of member avatar colors (used for the overlapping circles).
 */
data class Group(
    val id: String,
    val name: String,
    val members: List<Color> = emptyList(),
)
