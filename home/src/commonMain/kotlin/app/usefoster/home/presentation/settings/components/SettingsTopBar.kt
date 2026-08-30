package app.usefoster.home.presentation.settings.components

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
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_back
import foster.home.generated.resources.ic_edit
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.cd_back
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
        containerColor = FosterTheme.colors.background.b0,
        scrolledContainerColor = FosterTheme.colors.background.b0,
    ),
    title = {
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = FosterTheme.colors.text.primary,
            textAlign = TextAlign.Center,
        )
    },
    navigationIcon = {


        FilledIconButton(
            modifier = Modifier.size(40.dp),
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = FosterTheme.colors.background.b1,
                contentColor = FosterTheme.colors.text.primary
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
private fun PreviewContactProfileTopBar() = FosterTheme {

    Box(
        Modifier
            .fillMaxWidth()
            .background(FosterTheme.colors.background.b0),
    ) {
        SettingsTopBar(
            onBack = {},
        )
    }
}
