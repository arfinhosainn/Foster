package app.usenekko.onboarding.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.usenekko.theme.NekkoTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nekko",
            style = NekkoTheme.typography.heading1Bold,
            color = NekkoTheme.colors.text.primary,
        )
    }
}