package app.usenekko.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.usenekko.home.domain.Contact
import app.usenekko.home.domain.GroupMembership
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.avatar_blue
import nekko.home.generated.resources.avatar_green
import nekko.home.generated.resources.avatar_maroon
import nekko.home.generated.resources.avatar_orange
import nekko.home.generated.resources.avatar_red
import nekko.home.generated.resources.avatar_yellow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

private val avatarResources = listOf(
    Res.drawable.avatar_yellow,
    Res.drawable.avatar_green,
    Res.drawable.avatar_orange,
    Res.drawable.avatar_red,
    Res.drawable.avatar_maroon,
    Res.drawable.avatar_blue,
)

fun avatarIndexForColor(avatarColor: String?): Int? = when (avatarColor?.removePrefix("#")?.uppercase()) {
    "FFCC33" -> 0
    "34C759" -> 1
    "FF9500" -> 2
    "FF3B30" -> 3
    "AF52DE" -> 4
    "007AFF" -> 5
    else -> null
}

fun avatarResourceForColor(avatarColor: String?): DrawableResource? =
    avatarIndexForColor(avatarColor)?.let(avatarResources::get)

fun avatarIndexForId(selectedAvatarId: String?): Int? =
    selectedAvatarId?.toIntOrNull()?.takeIf { it in avatarResources.indices }

fun avatarResourceForId(selectedAvatarId: String?): DrawableResource? =
    avatarIndexForId(selectedAvatarId)?.let(avatarResources::get)

fun contactsForGroup(
    groupId: String,
    contacts: List<Contact>,
    memberships: List<GroupMembership>,
): List<Contact> {
    val memberIds = memberships
        .asSequence()
        .filter { it.groupId == groupId }
        .map { it.contactId }
        .toSet()
    return contacts.filter { it.id in memberIds }.take(if (memberIds.size > 6) 4 else 2)
}

fun groupMemberCount(
    groupId: String,
    memberships: List<GroupMembership>,
): Int = memberships.count { it.groupId == groupId }

@Composable
fun ContactAvatar(
    avatarColor: String?,
    modifier: Modifier = Modifier,
    fallbackColor: Color = NekkoTheme.colors.fill.secondary,
    selectedAvatarId: String? = null,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        (avatarResourceForId(selectedAvatarId) ?: avatarResourceForColor(avatarColor))?.let { resource ->
            Image(
                imageVector = vectorResource(resource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}