package app.usenekko.home.presentation.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.home.presentation.contactprofile.ContactProfileTopBar
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_back
import nekko.home.generated.resources.ic_edit
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.cd_back
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Settings",
    actions: @Composable () -> Unit = {},
) = CenterAlignedTopAppBar(
    modifier = modifier.padding(horizontal = 20.dp),
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = NekkoTheme.colors.background.b0,
        scrolledContainerColor = NekkoTheme.colors.background.b0,
    ),
    title = {
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = NekkoTheme.colors.text.primary,
            textAlign = TextAlign.Center,
        )
    },
    navigationIcon = {


        FilledIconButton(
            modifier = Modifier.size(40.dp),
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = NekkoTheme.colors.background.b1,
                contentColor = NekkoTheme.colors.text.primary
            ),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_back), contentDescription = stringResource(Res.string.cd_back),
                modifier = modifier.size(20.dp)

            )
        }
    },
    actions = {
        actions()
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
        SettingsTopBar(
            onBack = {},
        )
    }
}
