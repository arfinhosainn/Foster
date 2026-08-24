package app.usenekko.home.presentation.settings

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.adaptive.AdaptiveSurface
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.avatar_blue
import nekko.home.generated.resources.avatar_green
import nekko.home.generated.resources.avatar_maroon
import nekko.home.generated.resources.avatar_orange
import nekko.home.generated.resources.avatar_red
import nekko.home.generated.resources.avatar_yellow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.min
import nekko.home.generated.resources.add_choose_avatar
import nekko.home.generated.resources.add_select_avatar
import nekko.home.generated.resources.cd_avatar_number
import nekko.home.generated.resources.cd_close
import org.jetbrains.compose.resources.stringResource

private val profileAvatarResources: List<DrawableResource> = listOf(
    Res.drawable.avatar_yellow,
    Res.drawable.avatar_green,
    Res.drawable.avatar_orange,
    Res.drawable.avatar_red,
    Res.drawable.avatar_maroon,
    Res.drawable.avatar_blue,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseProfileAvatarBottomSheet(
    selectedAvatarIndex: Int?,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingAvatarIndex by rememberSaveable { mutableStateOf(selectedAvatarIndex) }
    val selectionRingBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFCC33), Color(0xFF34C759), Color(0xFFFFCC33)),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = NekkoTheme.colors.gray.quaternary) },
    ) {
        AdaptiveSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.add_choose_avatar),
                    style = NekkoTheme.typography.heading1Bold,
                    color = NekkoTheme.colors.text.primary,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NekkoTheme.colors.text.tertiary.copy(alpha = 0.2f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = NekkoTheme.colors.text.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            val columns = 3
            for (i in profileAvatarResources.indices step columns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (j in i until min(i + columns, profileAvatarResources.size)) {
                        val isSelected = pendingAvatarIndex == j
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, selectionRingBrush, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { pendingAvatarIndex = j },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(NekkoTheme.colors.fill.secondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    imageVector = vectorResource(profileAvatarResources[j]),
                                    contentDescription = stringResource(Res.string.cd_avatar_number, j + 1),
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(32.dp))

            NekkoButton(
                text = stringResource(Res.string.add_select_avatar),
                onClick = {
                    pendingAvatarIndex?.let {
                        onAvatarSelected(it)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            }
        }
    }
}