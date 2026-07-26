package app.usenekko.onboarding.contact.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.theme.NekkoTheme
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.avatar_blue
import nekko.onboarding.generated.resources.avatar_green
import nekko.onboarding.generated.resources.avatar_maroon
import nekko.onboarding.generated.resources.avatar_orange
import nekko.onboarding.generated.resources.avatar_red
import nekko.onboarding.generated.resources.avatar_yellow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.min

// Reordered to match the design grid: Yellow, Green, Orange / Red, Purple, Blue.
// NOTE: check that avatar_maroon is actually the purple asset in your drawables —
// if it renders as a wine/maroon tone instead of purple, you're likely missing
// a dedicated purple asset and this is just standing in for it.
val avatarResources: List<DrawableResource> = listOf(
    Res.drawable.avatar_yellow,
    Res.drawable.avatar_green,
    Res.drawable.avatar_orange,
    Res.drawable.avatar_red,
    Res.drawable.avatar_maroon,
    Res.drawable.avatar_blue,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAvatarBottomSheet(
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Gradient used for the selection ring — swap these two colors for real
    // theme tokens if you have accent/success colors defined elsewhere.
    val selectionRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NekkoTheme.colors.background.b0,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Choose Avatar",
                    style = NekkoTheme.typography.heading1Bold,
                    color = NekkoTheme.colors.text.primary
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.text.tertiary.copy(alpha = 0.2f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = NekkoTheme.colors.text.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
                                    .background(NekkoTheme.colors.fill.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    imageVector = vectorResource(avatarResources[j]),
                                    contentDescription = "Avatar ${j + 1}",
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            NekkoButton(
                text = "Select Avatar",
                onClick = {
                    selectedIndex?.let { onAvatarSelected(it) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}