package app.usefoster.home.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.cd_back_to_home
import foster.home.generated.resources.history_title
import foster.home.generated.resources.ic_back
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Top bar for the check-in history screen. History is the LEFT page of the
 * Home/History book, so the return affordance sits at the TRAILING end and
 * points toward Home (which lives to its right); tapping it slides focus back
 * to Home. Boards span years freely, so there is no year selector.
 */
@Composable
fun HistoryTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.history_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = FosterTheme.colors.text.primary,
            modifier = Modifier.weight(1f),
        )

        FilledIconButton(
            modifier = Modifier.size(40.dp),
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = FosterTheme.colors.background.b1,
                contentColor = FosterTheme.colors.text.primary,
            ),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_back),
                contentDescription = stringResource(Res.string.cd_back_to_home),
                modifier = Modifier
                    .size(20.dp)
                    // The book's return direction is toward Home on the right,
                    // so the arrow points trailing instead of leading.
                    .graphicsLayer { scaleX = -1f },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewHistoryTopBar() = FosterTheme {
    Box(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(FosterTheme.colors.background.b0),
    ) {
        HistoryTopBar(onBack = {})
    }
}
