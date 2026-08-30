package app.usefoster.home.presentation.contactprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_back
import foster.home.generated.resources.ic_edit
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.action_edit
import foster.home.generated.resources.cd_back
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactProfileActionBar(
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_back),
                contentDescription = stringResource(Res.string.cd_back),
                tint = FosterTheme.colors.gray.primary
            )
        }

        IconButton(
            onClick = onEditClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_edit),
                contentDescription = stringResource(Res.string.action_edit),
                tint = FosterTheme.colors.gray.primary
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewContactProfileActionBar() {
    ContactProfileActionBar(
        onBack = {},
        onEditClick = {},
    )
}
