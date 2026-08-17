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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.usenekko.theme.NekkoTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.gradients
import nekko.onboarding.generated.resources.ic_apple
import nekko.onboarding.generated.resources.ic_google
import nekko.onboarding.generated.resources.users
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun WelcomeScreen(
    supabaseClient: SupabaseClient,
    onGoogleSignInSuccess: () -> Unit = {},
    onAppleSignInSuccess: () -> Unit = {},
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
                Spacer(Modifier.height(20.dp))

                NekkoButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NekkoTheme.colors.background.b1,
                        contentColor = NekkoTheme.colors.background.onBackground
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_google),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                        )
                    },
                    text = "Continue with Google",
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
        WelcomeScreen(
            supabaseClient = io.github.jan.supabase.createSupabaseClient(
                supabaseUrl = "https://placeholder.supabase.co",
                supabaseKey = "placeholder"
            ) {}
        )
    }
}
