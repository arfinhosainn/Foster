package app.usefoster.home.addcontact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.shared.contacts.rememberContactPicker
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.action_finish
import foster.home.generated.resources.action_save
import foster.home.generated.resources.cd_close
import foster.home.generated.resources.ic_close
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Edit-contact sheet — the same wizard sheet used by the add-contact flow
 * ([AddContactSheetContent]), opened from the pen icon on the contact profile
 * screen.
 *
 * Differences from the add flow are intentionally minimal:
 *  - the corner control morphs: plain ✕ when there is nothing to save, plain
 *    "Save" text once the user has edited something — a bail-and-save that
 *    works from any step, and a subtle "unsaved edits" indicator;
 *  - the last-step footer button reads "Finish";
 *  - closing the sheet keeps the draft state, so re-opening resumes exactly
 *    where the user left off (state is only dropped when they leave the
 *    contact profile screen, which destroys the view model).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactSheet(
    viewModel: AddContactViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Corner Save and the last-step Finish button: persist what changed and
    // close. With nothing changed, they close without writing.
    val finishEditing = {
        if (state.hasChanges && state.canSubmit) {
            viewModel.submit()
        } else if (!state.isSubmitting) {
            onDismiss()
        }
        Unit
    }
    val launchContactPicker = rememberContactPicker(
        onContactSelected = viewModel::onContactImported,
        onPermissionDenied = {},
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddContactEvent.Saved -> {
                    // Fresh edit session next time the sheet opens.
                    viewModel.resetEditStep()
                    onSaved()
                }
                is AddContactEvent.ShowPaywall -> Unit
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FosterTheme.colors.background.b0,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle(
                    color = FosterTheme.colors.gray.quaternary,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp, top = 10.dp)
                        .wrapContentSize()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !state.isSubmitting,
                            onClick = if (state.hasChanges) finishEditing else onDismiss,
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.hasChanges) {
                        Text(
                            text = stringResource(Res.string.action_save),
                            style = FosterTheme.typography.heading4Semibold,
                            color = FosterTheme.colors.text.primary,
                        )
                    } else {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.cd_close),
                            tint = FosterTheme.colors.text.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) {
        AdaptiveSurface {
            AddContactSheetContent(
                state = state,
                isEditing = true,
                onNameChanged = viewModel::onNameChanged,
                onAvatarSelected = viewModel::onAvatarSelected,
                onGroupSelected = viewModel::onGroupSelected,
                onCreateGroupClicked = viewModel::onCreateGroupClicked,
                onFrequencySelected = viewModel::onFrequencySelected,
                onTimeSelected = viewModel::onTimeSelected,
                onTimeDialChanged = viewModel::onTimeDialChanged,
                onBackStep = viewModel::onBackStep,
                onNextStep = viewModel::onNextStep,
                onSubmit = finishEditing,
                onImportContact = launchContactPicker,
                submitLabelRes = Res.string.action_finish,
            )
        }
    }

    if (state.showCreateGroupSheet) {
        CreateGroupSheet(
            isSaving = state.isCreatingGroup,
            onDismiss = viewModel::onDismissCreateGroupSheet,
            onSave = viewModel::onSaveGroup,
        )
    }
}
