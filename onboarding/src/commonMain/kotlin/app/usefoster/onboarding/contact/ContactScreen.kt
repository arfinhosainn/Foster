package app.usefoster.onboarding.contact

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.blur
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
import app.usefoster.designsystem.buttons.FosterActionButton
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.home.presentation.components.ProfilePhotoPreview
import app.usefoster.shared.contacts.rememberContactPicker
import app.usefoster.onboarding.components.FosterStepField
import app.usefoster.onboarding.components.StepIndicator
import app.usefoster.designsystem.avatar.ChooseAvatarBottomSheet
import app.usefoster.designsystem.avatar.ProfilePhotoPicker
import app.usefoster.onboarding.presentation.rememberContactViewModel
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.ic_back
import foster.onboarding.generated.resources.ic_import
import org.jetbrains.compose.resources.vectorResource
import foster.onboarding.generated.resources.action_next
import foster.onboarding.generated.resources.add_contact_name_label
import foster.onboarding.generated.resources.add_import_contact
import foster.onboarding.generated.resources.add_step_new_contact_subtitle
import foster.onboarding.generated.resources.add_step_new_contact_title
import foster.onboarding.generated.resources.contact_import_failed
import foster.onboarding.generated.resources.error_contact_name_required
import org.jetbrains.compose.resources.stringResource

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
    var isPreviewVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val launchContactPicker = rememberContactPicker(
        onContactSelected = { contact ->
            viewModel.onAction(ContactAction.ContactImported(contact))
        },
        onPermissionDenied = {
            viewModel.onAction(ContactAction.ImportFailed)
        },
    )

    val contactImportFailedMessage = stringResource(Res.string.contact_import_failed)
    val contactNameRequiredMessage = stringResource(Res.string.error_contact_name_required)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ContactEvent.NavigateToNext -> onNavigateToNext()
                ContactEvent.NavigateBack -> onBack()
                ContactEvent.NavigateSkip -> onSkip()
                ContactEvent.NameRequired -> snackbarHostState.showSnackbar(
                    message = contactNameRequiredMessage,
                )
                ContactEvent.ImportFailed -> snackbarHostState.showSnackbar(
                    message = contactImportFailedMessage,
                )
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // Same treatment as CustomReminderScreen: blur the screen content while
    // the avatar picker bottom sheet is visible.
    val blurModifier = if (state.showAvatarPicker) Modifier.blur(20.dp) else Modifier

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FosterTheme.colors.background.b0)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(blurModifier),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FosterTheme.colors.background.b0,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        StepIndicator(
                            totalSteps = 7,
                            currentStep = 0,
                        )
                    },
                    navigationIcon = { },
                    actions = { },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                AdaptiveSurface {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FosterActionButton(
                            onClick = { viewModel.onAction(ContactAction.BackClicked) },
                            leadingIcon = vectorResource(Res.drawable.ic_back),
                            modifier = Modifier.weight(0.19f),
                        )
                        Spacer(Modifier.width(12.dp))
                        FosterButton(
                            text = stringResource(Res.string.action_next),
                            onClick = { viewModel.onAction(ContactAction.NextClicked) },
                            modifier = Modifier.weight(0.8f),
                        )
                    }
                }
            },
            containerColor = FosterTheme.colors.background.b0
        ) { innerPadding ->
            AdaptiveSurface(
                modifier = Modifier
                    .weight(1f)
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                Spacer(Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.add_step_new_contact_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = FosterTheme.colors.text.primary,
                        lineHeight = 38.sp,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(Res.string.add_step_new_contact_subtitle),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = FosterTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfilePhotoPicker(
                        photoBitmap = state.importedPhoto,
                        onEditClick = { viewModel.onShowAvatarPicker() },
                        onPreviewChanged = { isPreviewVisible = it },
                        selectedAvatarIndex = state.selectedAvatarIndex,
                    )
                }

                Spacer(Modifier.height(40.dp))

                FosterStepField(isConfirmed = false) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.contactName.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.add_contact_name_label),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FosterTheme.colors.text.tertiary,
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
                                fontFamily = FosterTheme.typography.bodyMedium.fontFamily,
                                color = FosterTheme.colors.text.primary,
                            ),
                            cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.onAction(ContactAction.NextClicked)
                                },
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { launchContactPicker() }
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
                        text = stringResource(Res.string.add_import_contact),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = FosterTheme.colors.text.secondary,
                    )
                }

                Spacer(Modifier.weight(1f))
            }
            }
        }
    }

    ProfilePhotoPreview(
        visible = isPreviewVisible,
        photoBitmap = state.importedPhoto,
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
    FosterTheme {
        ContactScreen(onNavigateToNext = {}, onBack = {}, onSkip = {})
    }
}
