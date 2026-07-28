package app.usenekko.onboarding.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.TermsAndPrivacyNotice
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.gradients
import nekko.onboarding.generated.resources.ic_apple
import nekko.onboarding.generated.resources.users
import org.jetbrains.compose.resources.painterResource

@Composable
fun WelcomeScreen(
    onNavigateToEmail: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .background(NekkoTheme.colors.background.b0)
                .blur(4.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.gradients),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopCenter),
                contentScale = ContentScale.FillBounds
            )

            Image(
                painter = painterResource(Res.drawable.users),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)

            )

            Image(
                painter = painterResource(Res.drawable.users),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd)
                    . scale(scaleX = -1f, scaleY = 1f)
            )

            Text(
                text = "Welcome Screen",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f).background(NekkoTheme.colors.background.b0),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.padding(horizontal = 40.dp)
            ) {
                Text(
                    text = "Simplest way to\nkeep in touch",
                    fontSize = 34.sp,
                    color = NekkoTheme.colors.background.onBackground,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Stay connected with those\nwho matters.",
                    fontSize = 20.sp,
                    color = NekkoTheme.colors.text.tertiary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(50.dp))


                NekkoButton(
                    text = "Continue with Apple",
                    onClick = {},
                    leadingIcon = {
                        Image(
                            painter = painterResource(Res.drawable.ic_apple),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(NekkoTheme.colors.background.b0)
                        )
                    },
                    loading = false
                )
                Spacer(Modifier.height(20.dp))

                NekkoButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NekkoTheme.colors.background.b1,
                        contentColor = NekkoTheme.colors.background.onBackground
                    ),
                    text = "Continue with Email",
                    onClick = onNavigateToEmail,
                    loading = false
                )
                Spacer(Modifier.height(25.dp))

                TermsAndPrivacyNotice(
                    onTermsClick = { /* navigate to Terms screen or open URL */ },
                    onPrivacyClick = { /* navigate to Privacy screen or open URL */ },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )

            }
        }
    }
}

@PreviewLightDark()
@Composable
fun WelcomeScreenPreview() {
    NekkoTheme {
        WelcomeScreen()

    }
}