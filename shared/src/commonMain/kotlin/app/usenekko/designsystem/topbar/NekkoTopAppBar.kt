package app.usenekko.designsystem.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.usenekko.theme.NekkoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NekkoTopAppBar(
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        },
        actions = trailingContent ?: {},
        modifier = modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = NekkoTheme.colors.background.b0,
        ),
    )
}
