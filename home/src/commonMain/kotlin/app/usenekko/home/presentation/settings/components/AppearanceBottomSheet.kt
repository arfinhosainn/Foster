package app.usenekko.home.presentation.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usenekko.theme.AppThemeMode
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.theme_dark
import nekko.home.generated.resources.theme_light
import nekko.home.generated.resources.theme_system
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class ThemeOption(
    val mode: AppThemeMode,
    val title: String,
    val preview: DrawableResource,
)

private val themeOptions = listOf(
    ThemeOption(AppThemeMode.DARK, "Dark", Res.drawable.theme_dark),
    ThemeOption(AppThemeMode.LIGHT, "Light", Res.drawable.theme_light),
    ThemeOption(AppThemeMode.SYSTEM, "System", Res.drawable.theme_system),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceBottomSheet(
    selectedMode: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Yellow/green selection ring, matching the Choose Avatar bottom sheet.
    val selectionRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
            Text(
                text = "Appearance",
                style = NekkoTheme.typography.heading1Bold,
                color = NekkoTheme.colors.text.primary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                themeOptions.forEach { option ->
                    ThemeOptionCard(
                        option = option,
                        isSelected = selectedMode == option.mode,
                        ringBrush = selectionRingBrush,
                        onClick = { onSelect(option.mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    option: ThemeOption,
    isSelected: Boolean,
    ringBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(320f / 616f)
                .clip(RoundedCornerShape(20.dp))
                .then(
                    if (isSelected) Modifier.border(
                        width = 3.dp,
                        brush = ringBrush,
                        shape = RoundedCornerShape(20.dp)
                    ) else Modifier
                )
                .background(NekkoTheme.colors.fill.secondary)
                .clickable(onClick = onClick)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(option.preview),
                contentDescription = option.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = option.title,
            style = NekkoTheme.typography.heading4Semibold,
            color = if (isSelected) NekkoTheme.colors.text.primary else NekkoTheme.colors.text.secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewAppearanceBottomSheet() = NekkoTheme {
    AppearanceBottomSheet(
        selectedMode = AppThemeMode.DARK,
        onSelect = {},
        onDismiss = {},
    )
}