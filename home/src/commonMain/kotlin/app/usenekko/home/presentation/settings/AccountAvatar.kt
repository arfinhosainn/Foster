package app.usenekko.home.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usenekko.home.presentation.components.ContactAvatar
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_pencil
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.cd_edit_profile_picture
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountAvatar(
    selectedAvatarId: String?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    Box(modifier = modifier.size(size)) {
        ContactAvatar(
            avatarColor = null,
            selectedAvatarId = selectedAvatarId,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 16.dp)
                .size(height = 24.dp, width = 36.dp)
                .clip(CircleShape)
                .border(2.dp, NekkoTheme.colors.stroke.secondary, CircleShape)
                .background(NekkoTheme.colors.background.b2)
                .clickable(onClick = onEditClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_pencil),
                contentDescription = stringResource(Res.string.cd_edit_profile_picture),
                tint = NekkoTheme.colors.background.onBackground,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
