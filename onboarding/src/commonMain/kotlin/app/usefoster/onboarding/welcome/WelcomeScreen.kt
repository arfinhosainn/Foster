package app.usefoster.onboarding.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.onboarding.components.TermsAndPrivacyNotice
import app.usefoster.onboarding.domain.OnboardingAuthError
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.onboarding.domain.toOnboardingAuthError
import app.usefoster.onboarding.presentation.toUserMessageResource
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.theme.FosterTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import kotlinx.coroutines.CancellationException
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithApple
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.auth_continue_apple
import foster.onboarding.generated.resources.auth_continue_google
import foster.onboarding.generated.resources.cd_google
import foster.onboarding.generated.resources.action_try_again
import foster.onboarding.generated.resources.gradients
import foster.onboarding.generated.resources.ic_apple
import foster.onboarding.generated.resources.ic_google
import foster.onboarding.generated.resources.logo_curve
import foster.onboarding.generated.resources.users
import foster.onboarding.generated.resources.welcome_placeholder
import foster.onboarding.generated.resources.welcome_subtitle
import foster.onboarding.generated.resources.welcome_title
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
    var errorMessage by androidx.compose.runtime.remember {
        mutableStateOf<OnboardingAuthError?>(
            null
        )
    }

    val googleSignInAction = supabaseClient.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    kotlin.io.println("GoogleSignIn Success!")
                    errorMessage = null
                    onGoogleSignInSuccess()
                }

                is NativeSignInResult.Error -> {
                    kotlin.io.println("GoogleSignIn failed")
                    errorMessage = OnboardingAuthError.Provider
                }

                is NativeSignInResult.NetworkError -> {
                    kotlin.io.println("GoogleSignIn network failure")
                    errorMessage = OnboardingAuthError.Network
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
                    kotlin.io.println("AppleSignIn failed")
                    errorMessage = OnboardingAuthError.Provider
                }

                is NativeSignInResult.NetworkError -> {
                    kotlin.io.println("AppleSignIn network failure")
                    errorMessage = OnboardingAuthError.Network
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
                .background(FosterTheme.colors.background.b0)
        ) {
            // 1) Blurred background layer — blur lives HERE, not on the parent
            Box(
                modifier = Modifier
                    .matchParentSize()
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
                        .offset(x = (0).dp, y = 50.dp)
                )
                Image(
                    painter = painterResource(Res.drawable.users),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .scale(scaleX = -1f, scaleY = 1f).offset(y = 50.dp)
                )
            }

            // 2) Sharp foreground logo — centered between the two user clusters
            Image(
                painter = painterResource(Res.drawable.logo_curve),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(120.dp).offset(y = 80.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f).background(FosterTheme.colors.background.b0),
            contentAlignment = Alignment.TopCenter
        ) {
            AdaptiveSurface {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {


                    Text(
                        text = stringResource(Res.string.welcome_title),
                        fontSize = 34.sp,
                        color = FosterTheme.colors.background.onBackground,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.welcome_subtitle),
                        fontSize = 18.sp,
                        color = FosterTheme.colors.text.tertiary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 25.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(50.dp))

                    FosterButton(
                        text = stringResource(Res.string.auth_continue_apple),
                        onClick = {
                            errorMessage = null
                            try {
                                println("Starting Apple Sign-In flow...")
                                appleSignInAction.startFlow()
                                println("Apple startFlow returned (no exception)")
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                println("Apple startFlow failed")
                                errorMessage = e.toOnboardingAuthError()
                            }
                        },
                        leadingIcon = {
                            Image(
                                painter = painterResource(Res.drawable.ic_apple),
                                contentDescription = "",
                                colorFilter = ColorFilter.tint(FosterTheme.colors.background.b0)
                            )
                        },
                        loading = false
                    )
                    Spacer(Modifier.height(13.dp))

                    FosterButton(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FosterTheme.colors.fill.tertiary,
                            contentColor = FosterTheme.colors.background.onBackground
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
                            errorMessage = null
                            try {
                                println("Starting Google Sign-In flow...")
                                googleSignInAction.startFlow()
                                println("startFlow returned (no exception)")
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                println("Google startFlow failed")
                                errorMessage = e.toOnboardingAuthError()
                            }
                        },
                        loading = false
                    )
                    errorMessage?.let {
                        Text(
                            text = stringResource(it.toUserMessageResource()),
                            color = FosterTheme.colors.red.default,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    profileLoadError?.let { error ->
                        Text(
                            text = stringResource(error.toUserMessageResource()),
                            color = FosterTheme.colors.red.default,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        FosterButton(
                            text = stringResource(Res.string.action_try_again),
                            onClick = onRetryProfileLoad,
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    TermsAndPrivacyNotice(
                        onTermsClick = { /* navigate to Terms screen or open URL */ },
                        onPrivacyClick = { /* navigate to Privacy screen or open URL */ },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewLightDark()
@Composable
fun WelcomeScreenPreview() {
    FosterTheme {
        WelcomeScreen(
            supabaseClient = io.github.jan.supabase.createSupabaseClient(
                supabaseUrl = "https://placeholder.supabase.co",
                supabaseKey = "placeholder"
            ) {}
        )
    }
}
