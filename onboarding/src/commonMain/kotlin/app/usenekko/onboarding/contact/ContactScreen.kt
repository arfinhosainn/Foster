package app.usenekko.onboarding.contact

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.NekkoStepField
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.contact.components.ChooseAvatarBottomSheet
import app.usenekko.onboarding.contact.components.ProfilePhotoPicker
import app.usenekko.onboarding.contact.components.ProfilePhotoPreview
import app.usenekko.onboarding.presentation.rememberContactViewModel
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import nekko.onboarding.generated.resources.ic_import
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberContactViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var photoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isPreviewVisible by remember { mutableStateOf(false) }

    var importContactName by remember { mutableStateOf<String?>(null) }

    val requestPermission = rememberContactPermissionLauncher(
        onGranted = {
            importContactName?.let { name ->
                viewModel.onAction(ContactAction.ContactNameChanged(name))
            }
        },
        onDenied = { }
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ContactEvent.NavigateToNext -> onNavigateToNext()
                ContactEvent.NavigateBack -> onBack()
                ContactEvent.NavigateSkip -> onSkip()
                ContactEvent.RequestContactPermission -> {
                    importContactName = "Imported Contact"
                    requestPermission()
                }
                is ContactEvent.ContactImported -> {
                    viewModel.onAction(ContactAction.ContactNameChanged(event.name))
                }
                ContactEvent.ContactPermissionDenied -> { }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NekkoTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        StepIndicator(
                            totalSteps = 8,
                            currentStep = 1,
                        )
                    },
                    navigationIcon = { },
                    actions = { },
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
                    FilledIconButton(
                        modifier = Modifier.weight(0.23f).size(58.dp),
                        onClick = { viewModel.onAction(ContactAction.BackClicked) },
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
                        onClick = { viewModel.onAction(ContactAction.NextClicked) },
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
                    .padding(horizontal = 30.dp)
            ) {
                Spacer(Modifier.height(42.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add a new contact",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = NekkoTheme.colors.text.primary,
                        lineHeight = 36.sp,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "You can fill out or import from your contacts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfilePhotoPicker(
                        photoBitmap = photoBitmap,
                        onEditClick = { viewModel.onShowAvatarPicker() },
                        onPreviewChanged = { isPreviewVisible = it },
                        selectedAvatarIndex = state.selectedAvatarIndex,
                    )
                }

                Spacer(Modifier.height(50.dp))

                NekkoStepField(isConfirmed = false) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.contactName.isEmpty()) {
                            Text(
                                text = "Contact name",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NekkoTheme.colors.text.tertiary,
                            )
                        }
                        BasicTextField(
                            value = state.contactName,
                            onValueChange = { viewModel.onAction(ContactAction.ContactNameChanged(it)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = NekkoTheme.typography.bodyMedium.fontFamily,
                                color = NekkoTheme.colors.text.primary,
                            ),
                            cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (state.contactName.isNotBlank()) {
                                        viewModel.onAction(ContactAction.NextClicked)
                                    }
                                },
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { viewModel.onAction(ContactAction.ImportClicked) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Image(
                        imageVector = vectorResource(Res.drawable.ic_import),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Import contact",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.secondary,
                    )
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }

    ProfilePhotoPreview(
        visible = isPreviewVisible,
        photoBitmap = photoBitmap,
    )

    if (state.showAvatarPicker) {
        ChooseAvatarBottomSheet(
            onAvatarSelected = { index -> viewModel.onAction(ContactAction.AvatarSelected(index)) },
            onDismiss = { viewModel.onDismissAvatarPicker() },
        )
    }
}

@PreviewLightDark
@Composable
private fun ContactScreenPreview() {
    NekkoTheme {
        ContactScreen(onNavigateToNext = {}, onBack = {}, onSkip = {})
    }
}
