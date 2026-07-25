package app.usenekko.onboarding.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.topbar.NekkoTopAppBar
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.checkin_img
import nekko.onboarding.generated.resources.checkin_imglight
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun NotificationScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(NotificationState()) }

    NotificationScreenContent(
        state = state,
        onAction = { action ->
            when (action) {
                is NotificationAction.TurnOnClicked -> {
                    state = state.copy(isNotificationEnabled = true)
                }
            }
        },
        onNavigateToNext = onNavigateToNext,
        onBack = onBack,
        onSkip = onSkip,
        modifier = modifier,
    )
}

@Composable
private fun NotificationScreenContent(
    state: NotificationState,
    onAction: (NotificationAction) -> Unit,
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val painter = if (isSystemInDarkTheme()) {
        painterResource(Res.drawable.checkin_img)
    } else {
        painterResource(Res.drawable.checkin_imglight)
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
    ) {
        NekkoTopAppBar(
            trailingContent = {
                Text(
                    text = "Skip",
                    color = NekkoTheme.colors.text.secondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .clickable { onSkip() },
                )
            },
        ) {
            StepIndicator(
                totalSteps = 6,
                currentStep = 3,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Get Notified",
                style = NekkoTheme.typography.heading1Bold,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = NekkoTheme.colors.text.primary,
            )
            Spacer(Modifier.height(15.dp))
            Text(
                text = "Keep up with check-ins &\nbirthdays with friends",
                style = NekkoTheme.typography.heading3Bold,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }


        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painter,
                contentDescription = "Check-in illustration",
                contentScale = ContentScale.Fit
            )
        }



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(bottom = 24.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Spacer(Modifier.width(12.dp))
            NekkoButton(
                text = "Turn on Notification",
                onClick = onNavigateToNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}


@PreviewLightDark
@Composable
fun PreviewNotificationScreen() {
    NekkoTheme {
        NotificationScreen(
            onNavigateToNext = {},
            onBack = {},
            onSkip = {},
        )
    }
}