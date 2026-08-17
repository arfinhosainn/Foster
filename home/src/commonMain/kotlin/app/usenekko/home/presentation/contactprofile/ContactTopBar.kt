package app.usenekko.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_back
import nekko.home.generated.resources.ic_edit
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ContactProfileTopBar(
    daysUntilNextCheckIn: Int,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) = CenterAlignedTopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = NekkoTheme.colors.background.b0,
        scrolledContainerColor = NekkoTheme.colors.background.b0,
    ),
    title = {
        Text(
            "$daysUntilNextCheckIn days to next\ncheck-in",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Normal,
            color = NekkoTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    },
    navigationIcon = {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_back),
                contentDescription = "Back",
                tint = NekkoTheme.colors.gray.primary
            )
        }

    },
    actions = {
        IconButton(
            onClick = onEditClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_edit),
                contentDescription = "Edit",
                tint = NekkoTheme.colors.gray.primary
            )
        }
    },
)

@PreviewLightDark
@Composable
private fun PreviewContactProfileTopBar() = NekkoTheme {

    Box(
        Modifier
            .fillMaxWidth()
            .background(NekkoTheme.colors.background.b0),
    ) {
        ContactProfileTopBar(
            daysUntilNextCheckIn = 12,
            onBack = {},
            onEditClick = {},
        )
    }
}
