package app.usenekko.designsystem.avatar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usenekko.theme.NekkoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nekko.shared.generated.resources.Res
import nekko.shared.generated.resources.cd_edit_profile_picture
import nekko.shared.generated.resources.cd_selected_avatar
import nekko.shared.generated.resources.cd_selected_photo
import nekko.shared.generated.resources.ic_pencil
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.milliseconds

/**
 * The app-wide profile photo/avatar picker. Shows the picked [photoBitmap],
 * falling back to the avatar at [selectedAvatarIndex] / [selectedAvatarId],
 * with the pencil edit badge at the bottom. Tap opens the editor; press-and-hold
 * zooms the avatar (for the fullscreen preview) with haptic feedback.
 *
 * This is the single source of truth for this UI — do not reimplement it in
 * feature modules.
 */
@Composable
fun ProfilePhotoPicker(
    photoBitmap: ImageBitmap?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 120.dp,
    selectedAvatarIndex: Int? = null,
    selectedAvatarId: String? = null,
    onPreviewChanged: (Boolean) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        if (zoom) 1.08f else 1f,
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "AvatarScaleAnimation"
    )

    val resolvedAvatarIndex = selectedAvatarIndex
        ?: selectedAvatarId?.toIntOrNull()?.takeIf { it in avatarResources.indices }

    Box(
        modifier = modifier
            .size(avatarSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(4.dp, NekkoTheme.colors.stroke.secondary, CircleShape)
                .background(NekkoTheme.colors.fill.secondary)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onEditClick() },
                        onPress = {
                            val job = scope.launch {
                                delay(300.milliseconds)
                                zoom = true
                                onPreviewChanged(true)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            try {
                                awaitRelease()
                            } finally {
                                job.cancel()
                                zoom = false
                                onPreviewChanged(false)
                            }
                        }
                    )
                }
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = stringResource(Res.string.cd_selected_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (resolvedAvatarIndex != null && resolvedAvatarIndex in avatarResources.indices) {
                Image(
                    imageVector = vectorResource(avatarResources[resolvedAvatarIndex]),
                    contentDescription = stringResource(Res.string.cd_selected_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NekkoTheme.colors.fill.secondary)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp)
                .size(height = 24.dp, width = 36.dp)
                .clip(CircleShape)
                .border(2.dp, NekkoTheme.colors.stroke.secondary.copy(alpha = 0.03f), CircleShape)
                .background(NekkoTheme.colors.background.b2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_pencil),
                contentDescription = stringResource(Res.string.cd_edit_profile_picture),
                tint = NekkoTheme.colors.text.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}