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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.designsystem.topbar.NekkoTopAppBar
import app.usenekko.onboarding.components.NekkoStepField
import app.usenekko.onboarding.components.StepIndicator
import app.usenekko.onboarding.photo.ProfilePhotoPicker
import app.usenekko.onboarding.photo.ProfilePhotoPreview
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_back
import nekko.onboarding.generated.resources.ic_import
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ContactScreen(
    onNavigateToNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var contactName by remember { mutableStateOf("") }
    var isPreviewVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NekkoTopAppBar(
            trailingContent = {
                Text(
                    text = "Skip",
                    color = NekkoTheme.colors.text.primary,
                    fontSize = 16.sp,
                )

            }
        ) {
            StepIndicator(
                totalSteps = 4,
                currentStep = 3,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 30.dp)
        ) {
            Spacer(Modifier.height(16.dp))

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

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "You can fill out or import from your contacts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary,
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
                    onEditClick = { /* TODO: image picker */ },
                    onPreviewChanged = { isPreviewVisible = it },
                )
            }

            Spacer(Modifier.height(50.dp))

            NekkoStepField(isConfirmed = false) {
                Box(modifier = Modifier.weight(1f)) {
                    if (contactName.isEmpty()) {
                        Text(
                            text = "Contact name",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NekkoTheme.colors.text.tertiary,
                        )
                    }
                    BasicTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NekkoTheme.colors.text.primary,
                        ),
                        cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (contactName.isNotBlank()) onNavigateToNext() },
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { /* TODO: import contacts */ }
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
                    color = NekkoTheme.colors.text.primary,
                )
            }

            Spacer(Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                modifier = modifier.weight(0.23f).size(58.dp),
                onClick = onBack,
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
                onClick = onNavigateToNext,
                modifier = Modifier.weight(0.8f),
            )
        }
    }

    ProfilePhotoPreview(
        visible = isPreviewVisible,
        photoBitmap = photoBitmap,
    )
}

@PreviewLightDark
@Composable
private fun ContactScreenPreview() {
    NekkoTheme {
        ContactScreen(onNavigateToNext = {}, onBack = {}, onSkip = {})
    }
}
