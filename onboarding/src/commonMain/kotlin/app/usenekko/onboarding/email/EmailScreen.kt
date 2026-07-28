package app.usenekko.onboarding.email

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.presentation.rememberEmailViewModel
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

@Composable
fun EmailScreen(
    onNavigateToEmailVerification: (email: String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberEmailViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EmailEvent.NavigateBack -> onBack()
                EmailEvent.NavigateSkip -> onSkip()
                is EmailEvent.NavigateToEmailVerification -> {
                    onNavigateToEmailVerification(event.email)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
            .imePadding()
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = { },
                    navigationIcon = { },
                    actions = {
                        Button(
                            onClick = { viewModel.onAction(EmailAction.SkipClicked) },
                            colors = ButtonDefaults.buttonColors(containerColor = NekkoTheme.colors.background.b0)
                        ) {
                            Text(
                                text = "Skip",
                                color = NekkoTheme.colors.text.secondary,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledIconButton(
                        modifier = modifier.weight(0.23f).size(58.dp),
                        onClick = { viewModel.onAction(EmailAction.BackClicked) },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = NekkoTheme.colors.fill.tertiary)
                    ) {
                        Image(
                            imageVector = vectorResource(Res.drawable.ic_back),
                            contentDescription = "BACK"
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    NekkoButton(
                        text = "Next",
                        onClick = { viewModel.onAction(EmailAction.ContinueClicked) },
                        modifier = Modifier.weight(0.8f),
                    )
                }
            },
            containerColor = NekkoTheme.colors.background.b0
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "What's your email?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = NekkoTheme.colors.text.primary,
                    )
                }

                Spacer(Modifier.height(30.dp))

                TextField(
                    value = state.email,
                    onValueChange = { viewModel.onAction(EmailAction.EmailChanged(it)) },
                    placeholder = {
                        Text(
                            text = "Enter your email",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = NekkoTheme.colors.text.tertiary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    shape = RoundedCornerShape(25.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.primary,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (state.email.isNotBlank()) {
                                viewModel.onAction(EmailAction.ContinueClicked)
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NekkoTheme.colors.fill.secondary,
                        unfocusedContainerColor = NekkoTheme.colors.fill.secondary,
                        disabledContainerColor = NekkoTheme.colors.fill.secondary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = NekkoTheme.colors.text.primary,
                    ),
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewEmailScreen() {
    NekkoTheme {
        EmailScreen(
            onNavigateToEmailVerification = {},
            onBack = {},
            onSkip = {},
        )
    }
}
