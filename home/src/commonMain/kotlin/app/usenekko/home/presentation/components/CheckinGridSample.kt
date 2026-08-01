package app.usenekko.home.presentation.components


import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.usenekko.theme.NekkoTheme


@PreviewLightDark
@Composable
private fun CheckInScreenPreview() {
    NekkoTheme {
        Surface{
            CheckInTimelineGridSample(
            )

        }
    }
}
