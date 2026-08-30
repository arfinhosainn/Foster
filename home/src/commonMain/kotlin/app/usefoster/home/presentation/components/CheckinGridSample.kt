package app.usefoster.home.presentation.components


import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.usefoster.theme.FosterTheme


@PreviewLightDark
@Composable
private fun CheckInScreenPreview() {
    FosterTheme {
        Surface{
            CheckInTimelineGridSample(
            )

        }
    }
}
