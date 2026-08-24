package app.usenekko.home.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_close
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.action_cancel
import nekko.home.generated.resources.action_delete
import nekko.home.generated.resources.cd_close
import nekko.home.generated.resources.delete_type_confirm
import nekko.home.generated.resources.settings_delete_account
import org.jetbrains.compose.resources.stringResource
import nekko.home.generated.resources.delete_warning_body

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmedText by rememberSaveable { mutableStateOf("") }
    val confirmed = !isLoading && confirmedText.uppercase() == "DELETE"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth()) {
                BottomSheetDefaults.DragHandle(
                    color = NekkoTheme.colors.gray.quaternary,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp, top = 10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Unspecified)
                        .clickable(enabled = !isLoading) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = NekkoTheme.colors.gray.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.settings_delete_account),
                    style = NekkoTheme.typography.heading1Bold,
                    color = NekkoTheme.colors.red.default,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(Res.string.delete_warning_body),
                style = NekkoTheme.typography.bodyMedium,
                color = NekkoTheme.colors.text.secondary,
            )

            Spacer(Modifier.height(12.dp))

            val lostItems = listOf(
                "Contacts and groups",
                "Notes and reminders",
                "Check-in history and streaks",
                "Badges and progress",
                "Subscription and notification settings",
            )
            lostItems.forEach { item ->
                Text(
                    text = "•  $item",
                    style = NekkoTheme.typography.bodyMedium,
                    color = NekkoTheme.colors.text.secondary,
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.delete_type_confirm),
                style = NekkoTheme.typography.heading4Semibold,
                color = NekkoTheme.colors.text.primary,
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmedText,
                onValueChange = { confirmedText = it.uppercase() },
                singleLine = true,
                enabled = !isLoading,
                textStyle = NekkoTheme.typography.heading4Semibold,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "DELETE",
                        style = NekkoTheme.typography.heading4Semibold,
                        color = NekkoTheme.colors.text.tertiary,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NekkoTheme.colors.red.default,
                    unfocusedBorderColor = NekkoTheme.colors.stroke.primary,
                    focusedContainerColor = NekkoTheme.colors.background.b0,
                    unfocusedContainerColor = NekkoTheme.colors.background.b0,
                    cursorColor = NekkoTheme.colors.text.primary,
                ),
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = Color(0xFFFF4B4B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NekkoButton(
                    text = stringResource(Res.string.action_cancel),
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NekkoTheme.colors.fill.tertiary,
                        contentColor = NekkoTheme.colors.text.primary,
                    ),
                )
                NekkoButton(
                    text = stringResource(Res.string.action_delete),
                    onClick = onConfirm,
                    enabled = confirmed,
                    loading = isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4B4B),
                        contentColor = Color.White,
                        disabledContainerColor = NekkoTheme.colors.red.active.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f),
                    ),
                )
            }
            }
        }
    }
}