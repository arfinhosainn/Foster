package app.usenekko.home.addcontact

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.shared.contacts.rememberContactPicker
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.cd_close
import nekko.home.generated.resources.edit_contact_title
import nekko.home.generated.resources.edit_discard_body
import nekko.home.generated.resources.edit_discard_title
import nekko.home.generated.resources.edit_keep_editing
import nekko.home.generated.resources.edit_save_changes
import nekko.home.generated.resources.edit_subtitle
import org.jetbrains.compose.resources.stringResource
import nekko.home.generated.resources.edit_discard_confirm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactSheet(
    viewModel: AddContactViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestDismiss = {
        if (state.hasChanges && !state.isSubmitting) {
            showDiscardDialog = true
        } else if (!state.isSubmitting) {
            onDismiss()
        }
    }
    val launchContactPicker = rememberContactPicker(
        onContactSelected = viewModel::onContactImported,
        onPermissionDenied = {},
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddContactEvent.Saved -> onSaved()
                is AddContactEvent.ShowPaywall -> Unit
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NekkoTheme.colors.background.b0,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
        modifier = modifier,
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.edit_contact_title),
                        style = NekkoTheme.typography.heading2,
                        fontWeight = FontWeight.SemiBold,
                        color = NekkoTheme.colors.text.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    IconButton(
                        onClick = requestDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.cd_close),
                            tint = NekkoTheme.colors.text.primary,
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.edit_subtitle),
                    style = NekkoTheme.typography.bodyMedium,
                    color = NekkoTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(28.dp))
                EditSectionTitle("Basic details")
                NameAndAvatarStep(
                    state = state,
                    onNameChanged = viewModel::onNameChanged,
                    onAvatarSelected = viewModel::onAvatarSelected,
                    onImportContact = launchContactPicker,
                )

                Spacer(Modifier.height(28.dp))
                EditSectionTitle("Relationship group")
                GroupStep(
                    groups = state.groups,
                    contacts = state.contacts,
                    memberships = state.memberships,
                    groupsLoading = state.groupsLoading,
                    selectedGroupId = state.selectedGroupId,
                    pendingAvatarColor = AddContactViewModel.colorHexes[state.selectedAvatarIndex ?: 0],
                    onGroupSelected = viewModel::onGroupSelected,
                    onCreateGroupClicked = viewModel::onCreateGroupClicked,
                )

                Spacer(Modifier.height(28.dp))
                EditSectionTitle("Check-in schedule")
                FrequencyStep(
                    state = state,
                    onFrequencySelected = viewModel::onFrequencySelected,
                )
                Spacer(Modifier.height(24.dp))
                ReminderTimeStep(
                    state = state,
                    onTimeSelected = viewModel::onTimeSelected,
                    onTimeDialChanged = viewModel::onTimeDialChanged,
                )

                state.error?.let { error ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        color = NekkoTheme.colors.red.default,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(28.dp))
                NekkoButton(
                    text = stringResource(Res.string.edit_save_changes),
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSaveChanges,
                    loading = state.isSubmitting,
                )
            }
        }
    }

    if (state.showCreateGroupSheet) {
        CreateGroupSheet(
            isSaving = state.isCreatingGroup,
            onDismiss = viewModel::onDismissCreateGroupSheet,
            onSave = viewModel::onSaveGroup,
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(Res.string.edit_discard_title)) },
            text = { Text(stringResource(Res.string.edit_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    },
                ) {
                    Text(stringResource(Res.string.edit_discard_confirm), color = NekkoTheme.colors.red.default)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(Res.string.edit_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun EditSectionTitle(text: String) {
    Text(
        text = text,
        style = NekkoTheme.typography.heading4Semibold,
        color = NekkoTheme.colors.text.primary,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}