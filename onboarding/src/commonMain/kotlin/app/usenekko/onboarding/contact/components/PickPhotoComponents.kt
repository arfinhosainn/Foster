package app.usenekko.onboarding.contact.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usenekko.theme.NekkoTheme
import app.usenekko.home.presentation.components.ProfilePhotoPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nekko.onboarding.generated.resources.Res
import nekko.onboarding.generated.resources.ic_pencil
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ProfilePhotoPicker(
    photoBitmap: ImageBitmap?,
    onEditClick: () -> Unit,
    onPreviewChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 130.dp,
    selectedAvatarIndex: Int? = null,
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
                    contentDescription = "Selected Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (selectedAvatarIndex != null && selectedAvatarIndex in avatarResources.indices) {
                Image(
                    imageVector = vectorResource(avatarResources[selectedAvatarIndex]),
                    contentDescription = "Selected Avatar",
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
                .offset(y = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, NekkoTheme.colors.stroke.secondary, CircleShape)
                .background(NekkoTheme.colors.background.b2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_pencil),
                contentDescription = "Edit Profile Picture",
                tint = NekkoTheme.colors.text.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePhotoPickerPreview() {
    Surface {
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            ProfilePhotoPicker(
                photoBitmap = null,
                onEditClick = {},
                onPreviewChanged = {}
            )
        }
    }
}

@Preview
@Composable
private fun ProfilePhotoPreviewComponentPreview() {

    NekkoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background content to show transparency
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
            ProfilePhotoPreview(
                visible = true,
                photoBitmap = null
            )
        }

    }
}
