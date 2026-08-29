package app.usenekko.onboarding.name

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.presentation.rememberNameViewModel
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import nekko.onboarding.generated.resources.action_next
import nekko.onboarding.generated.resources.cd_logo
import nekko.onboarding.generated.resources.error_name_required
import nekko.onboarding.generated.resources.name_full_name
import nekko.onboarding.generated.resources.name_what_call
import org.jetbrains.compose.resources.stringResource

@Composable
fun NameScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberNameViewModel()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val nameRequiredMessage = stringResource(Res.string.error_name_required)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                NameEvent.NavigateToNext -> onNavigateToNext()
                NameEvent.NavigateBack -> onBack()
                NameEvent.NavigateSkip -> onSkip()
                NameEvent.NameRequired -> snackbarHostState.showSnackbar(
                    message = nameRequiredMessage,
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
            .imePadding()
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                    },
                    navigationIcon = { },
                    actions = {

                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NekkoButton(
                        text = stringResource(Res.string.action_next),
                        onClick = { viewModel.onContinueClicked() },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
            containerColor = NekkoTheme.colors.background.b0
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding)
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(Res.drawable.ic_logo),
                    contentDescription = stringResource(Res.string.cd_logo),
                )
                Spacer(Modifier.height(0.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.name_what_call),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = NekkoTheme.colors.text.primary,
                        lineHeight = 36.sp,
                    )
                }

                Spacer(Modifier.height(24.dp))

                NameField(
                    value = name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    onDone = { viewModel.onContinueClicked() },
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit = {},
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = stringResource(Res.string.name_full_name),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.tertiary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        shape = RoundedCornerShape(25.dp),
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = NekkoTheme.typography.heading3.fontFamily,
            color = NekkoTheme.colors.text.primary,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
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
}

@PreviewLightDark
@Composable
private fun NameScreenPreview() {
    NekkoTheme {
        NameScreen(onNavigateToNext = {}, onBack = {}, onSkip = {})
    }
}
