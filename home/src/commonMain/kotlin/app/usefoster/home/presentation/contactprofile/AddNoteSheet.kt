package app.usefoster.home.presentation.contactprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.adaptive.AdaptiveSurface
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.ic_close
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.action_save
import foster.home.generated.resources.cd_close
import foster.home.generated.resources.notes_add_note
import foster.home.generated.resources.reminder_description_label
import org.jetbrains.compose.resources.stringResource
import foster.home.generated.resources.field_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheet(
    draftTitle: String,
    draftDescription: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FosterTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
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
                        .clip(CircleShape)
                        .background(Color.Unspecified)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = FosterTheme.colors.gray.secondary,
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .imePadding(),
            ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.notes_add_note),
                    style = FosterTheme.typography.heading3Bold,
                    color = FosterTheme.colors.text.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(FosterTheme.colors.fill.tertiary)
                    .padding(20.dp),
            ) {
                Column {
                    BasicTextField(
                        value = draftTitle,
                        onValueChange = onTitleChanged,
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            color = FosterTheme.colors.text.tertiary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FosterTheme.typography.heading3.fontFamily,
                        ),
                        cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (draftTitle.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.field_title),
                                    fontSize = 20.sp,
                                    color = FosterTheme.colors.text.tertiary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            innerTextField()
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(FosterTheme.colors.gray.quaternary),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = draftDescription,
                        onValueChange = onDescriptionChanged,
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            color = FosterTheme.colors.text.primary,
                            fontFamily = FosterTheme.typography.bodyMedium.fontFamily,
                        ),
                        cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        decorationBox = { innerTextField ->
                            if (draftDescription.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.reminder_description_label),
                                    fontSize = 17.sp,
                                    color = FosterTheme.colors.text.quaternary,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FosterButton(
                text = stringResource(Res.string.action_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = draftTitle.isNotBlank() && !isSaving,
                loading = isSaving,
            )
            }
        }
    }
}
