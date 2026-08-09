package app.usenekko.home.presentation.components

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_pencil
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ProfilePhotoPreview(
    visible: Boolean,
    photoBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    photoSize: Dp = 340.dp,
    fullScreen: Boolean = true,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        modifier = if (fullScreen) modifier else modifier.size(photoSize),
        enter = fadeIn() + scaleIn(initialScale = .92f),
        exit = fadeOut() + scaleOut(targetScale = .92f),
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow,
            ),
            label = "AvatarPreviewScaleAnimation",
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (fullScreen) Color.Black.copy(alpha = .82f)
                    else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val imageModifier = Modifier
                .size(photoSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .then(
                    if (fullScreen) Modifier
                    else Modifier
                        .border(4.dp, NekkoTheme.colors.stroke.secondary, CircleShape)
                        .clickable(onClick = onEditClick),
                )

            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = "Full Screen Photo Preview",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = imageModifier.background(
                        if (fullScreen) Color(0xFFE5E5E5)
                        else NekkoTheme.colors.fill.secondary,
                    ),
                )
            }

            if (!fullScreen && showEditButton) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(2.dp, NekkoTheme.colors.stroke.secondary, CircleShape)
                        .background(NekkoTheme.colors.background.b2)
                        .clickable(onClick = onEditClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_pencil),
                        contentDescription = "Edit profile picture",
                        tint = NekkoTheme.colors.background.onBackground,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}