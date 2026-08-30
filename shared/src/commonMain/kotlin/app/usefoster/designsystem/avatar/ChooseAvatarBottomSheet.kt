package app.usefoster.designsystem.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.theme.FosterTheme
import foster.shared.generated.resources.Res
import foster.shared.generated.resources.add_choose_avatar
import foster.shared.generated.resources.add_select_avatar
import foster.shared.generated.resources.avatar_blue
import foster.shared.generated.resources.avatar_green
import foster.shared.generated.resources.avatar_maroon
import foster.shared.generated.resources.avatar_orange
import foster.shared.generated.resources.avatar_red
import foster.shared.generated.resources.avatar_yellow
import foster.shared.generated.resources.cd_avatar_number
import foster.shared.generated.resources.cd_close
import foster.shared.generated.resources.ic_close
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.min

// Reordered to match the design grid: Yellow, Green, Orange / Red, Purple, Blue.
// NOTE: check that avatar_maroon is actually the purple asset in your drawables —
// if it renders as a wine/maroon tone instead of purple, you're likely missing
// a dedicated purple asset and this is just standing in for it.
// Indexes here are the app-wide avatar IDs (persisted as "0".."5").
val avatarResources: List<DrawableResource> = listOf(
    Res.drawable.avatar_yellow,
    Res.drawable.avatar_green,
    Res.drawable.avatar_orange,
    Res.drawable.avatar_red,
    Res.drawable.avatar_maroon,
    Res.drawable.avatar_blue,
)

/**
 * The one avatar picker sheet, shared by onboarding, add-contact and account
 * settings. Visually: two pill rows of avatars in a rounded sheet with a
 * gradient selection ring and a full-width select button.
 *
 * @param selectedAvatarIndex optionally pre-selects an avatar (account editing);
 * pass nothing for a fresh selection (onboarding / new contact).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAvatarBottomSheet(
    selectedAvatarIndex: Int? = null,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIndex by rememberSaveable { mutableStateOf(selectedAvatarIndex) }

    // Gradient used for the selection ring — swap these two colors for real
    // theme tokens if you have accent/success colors defined elsewhere.
    val selectionRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33))
    )

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
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = FosterTheme.colors.gray.secondary,
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.add_choose_avatar),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FosterTheme.colors.text.primary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            val columns = 3
            for (i in avatarResources.indices step columns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (j in i until min(i + columns, avatarResources.size)) {
                        val isSelected = selectedIndex == j
                        // Outer box reserves the halo gap + ring, always the same
                        // size regardless of selection so the grid never shifts.
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 3.dp,
                                        brush = selectionRingBrush,
                                        shape = CircleShape
                                    ) else Modifier
                                )
                                .clickable { selectedIndex = j },
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner box is the actual avatar circle, smaller than
                            // the outer box so the ring floats outside it with a gap.
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(FosterTheme.colors.fill.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    imageVector = vectorResource(avatarResources[j]),
                                    contentDescription = stringResource(Res.string.cd_avatar_number, j + 1),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            FosterButton(
                text = stringResource(Res.string.add_select_avatar),
                onClick = {
                    selectedIndex?.let { index ->
                        onAvatarSelected(index)
                        onDismiss()
                    }
                },
                enabled = selectedIndex != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
