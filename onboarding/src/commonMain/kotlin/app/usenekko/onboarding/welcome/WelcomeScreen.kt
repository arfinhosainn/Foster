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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.TermsAndPrivacyNotice
import app.usenekko.onboarding.domain.OnboardingProfileError
import app.usenekko.onboarding.domain.toUserMessage
import app.usenekko.theme.NekkoTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.auth_continue_apple
import nekko.onboarding.generated.resources.auth_continue_google
import nekko.onboarding.generated.resources.cd_google
import nekko.onboarding.generated.resources.gradients
import nekko.onboarding.generated.resources.ic_apple
import nekko.onboarding.generated.resources.ic_google
import nekko.onboarding.generated.resources.users
import nekko.onboarding.generated.resources.welcome_placeholder
import nekko.onboarding.generated.resources.welcome_subtitle
import nekko.onboarding.generated.resources.welcome_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun WelcomeScreen(
    supabaseClient: SupabaseClient,
    onGoogleSignInSuccess: () -> Unit = {},
    onAppleSignInSuccess: () -> Unit = {},
    profileLoadError: OnboardingProfileError? = null,
    onRetryProfileLoad: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var errorMessage by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }

    val googleSignInAction = supabaseClient.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    kotlin.io.println("GoogleSignIn Success!")
                    errorMessage = null
                    onGoogleSignInSuccess()
                }
                is NativeSignInResult.Error -> {
                    kotlin.io.println("GoogleSignIn Error: ${result.message}")
                    errorMessage = result.message
                }
                is NativeSignInResult.NetworkError -> {
                    kotlin.io.println("GoogleSignIn NetworkError: ${result.message}")
                    errorMessage = "Network error. Check your connection."
                }
                NativeSignInResult.ClosedByUser -> {
                    kotlin.io.println("GoogleSignIn: User cancelled")
                }
            }
        },
    )
    val appleSignInAction = supabaseClient.composeAuth.rememberSignInWithApple(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    kotlin.io.println("AppleSignIn Success!")
                    errorMessage = null
                    onAppleSignInSuccess()
                }
                is NativeSignInResult.Error -> {
                    kotlin.io.println("AppleSignIn Error: ${result.message}")
                    errorMessage = result.message
                }
                is NativeSignInResult.NetworkError -> {
                    kotlin.io.println("AppleSignIn NetworkError: ${result.message}")
                    errorMessage = "Network error. Check your connection."
                }
                NativeSignInResult.ClosedByUser -> {
                    kotlin.io.println("AppleSignIn: User cancelled")
                }
            }
        },
    )

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
                    .scale(scaleX = -1f, scaleY = 1f)
            )

            Text(
                text = stringResource(Res.string.welcome_placeholder),
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
                    text = stringResource(Res.string.welcome_title),
                    fontSize = 34.sp,
                    color = NekkoTheme.colors.background.onBackground,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.welcome_subtitle),
                    fontSize = 18.sp,
                    color = NekkoTheme.colors.text.tertiary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(50.dp))

                NekkoButton(
                    text = stringResource(Res.string.auth_continue_apple),
                    onClick = {
                        try {
                            println("Starting Apple Sign-In flow...")
                            appleSignInAction.startFlow()
                            println("Apple startFlow returned (no exception)")
                        } catch (e: Exception) {
                            println("Apple startFlow threw: ${e.message}")
                            errorMessage = "Error: ${e.message}"
                        }
                    },
                    leadingIcon = {
                        Image(
                            painter = painterResource(Res.drawable.ic_apple),
                            contentDescription = "",
                            colorFilter = ColorFilter.tint(NekkoTheme.colors.background.b0)
                        )
                    },
                    loading = false
                )
                Spacer(Modifier.height(13.dp))

                NekkoButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NekkoTheme.colors.fill.tertiary,
                        contentColor = NekkoTheme.colors.background.onBackground
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_google),
                            contentDescription = stringResource(Res.string.cd_google),
                            tint = Color.Unspecified,
                        )
                    },
                    text = stringResource(Res.string.auth_continue_google),
                    onClick = {
                        try {
                            println("Starting Google Sign-In flow...")
                            googleSignInAction.startFlow()
                            println("startFlow returned (no exception)")
                        } catch (e: Exception) {
                            println("startFlow threw: ${e.message}")
                            errorMessage = "Error: ${e.message}"
                        }
                    },
                    loading = false
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = NekkoTheme.colors.red.default,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                profileLoadError?.let { error ->
                    Text(
                        text = error.toUserMessage(),
                        color = NekkoTheme.colors.red.default,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    NekkoButton(text = "Try again", onClick = onRetryProfileLoad)
                }

                Spacer(Modifier.height(24.dp))

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
        WelcomeScreen(
            supabaseClient = io.github.jan.supabase.createSupabaseClient(
                supabaseUrl = "https://placeholder.supabase.co",
                supabaseKey = "placeholder"
            ) {}
        )
    }
}
