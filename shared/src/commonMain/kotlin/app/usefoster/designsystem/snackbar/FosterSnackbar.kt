package app.usefoster.designsystem.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usefoster.theme.FosterTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Semantic style for [FosterSnackbar]. A host renders one style, so screens
 * pick the style matching what their host shows (validation errors → Error,
 * confirmations → Success, everything else → Neutral).
 */
enum class FosterSnackbarStyle {
    /** Dark pill, no icon — the default look (matches design reference). */
    Neutral,

    /** Green pill with a check icon — confirmations ("Saved", "Sent"). */
    Success,

    /** Red pill with a warning icon — validation and failure messages. */
    Error,
}

/**
 * Minimal [SnackbarVisuals] for Foster snackbars. Actions, dismiss ✕ and
 * secondary labels aren't rendered by design — the pill is message-only.
 */
data class FosterSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

/**
 * The one snackbar look for the whole app: a colored pill with Foster
 * typography and an optional semantic icon, message only and horizontally
 * centered. Screens should render it through [FosterSnackbarHost]
 * instead of Material3's [androidx.compose.material3.SnackbarHost] so the
 * style stays consistent.
 */
@Composable
fun FosterSnackbar(
    data: SnackbarData,
    style: FosterSnackbarStyle = FosterSnackbarStyle.Neutral,
    modifier: Modifier = Modifier,
) {
    val containerColor: Color
    val contentColor: Color
    val accentIcon: ImageVector?

    when (style) {
        FosterSnackbarStyle.Neutral -> {
            // Fixed mid-gray pill with ink text — reads correctly in both
            // light and dark themes.
            containerColor = Color(0xFFA1A1AA)
            contentColor = Color(0xFF18181B)
            accentIcon = null
        }

        FosterSnackbarStyle.Success -> {
            containerColor = FosterTheme.colors.green.default
            contentColor = Color.White
            accentIcon = Icons.Filled.CheckCircle
        }

        FosterSnackbarStyle.Error -> {
            containerColor = FosterTheme.colors.red.default
            contentColor = Color.White
            accentIcon = Icons.Filled.Warning
        }
    }

    Snackbar(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (accentIcon != null) {
                Icon(
                    imageVector = accentIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
            }

            // Message only, centered — no actions, no dismiss ✕.
            Text(
                text = data.visuals.message,
                style = FosterTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Drop-in replacement for Material3's [androidx.compose.material3.SnackbarHost]
 * that renders every snackbar with [FosterSnackbar]. Accepts the same
 * [SnackbarHostState] call sites already use, so migrating a screen is just
 * swapping the host composable and picking a [style].
 *
 * The pill is inset generously from the screen edges, capped in width so it
 * never spans the whole screen, and floated ~30% up from the bottom. It
 * animates in/out with a fade plus a gentle vertical slide instead of snapping
 * into place.
 */
@Composable
fun FosterSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    style: FosterSnackbarStyle = FosterSnackbarStyle.Neutral,
) {
    val data = hostState.currentSnackbarData
    // Keep the last non-null item around so the exit animation has something
    // to render while the snackbar fades out.
    var lastData by remember { mutableStateOf<SnackbarData?>(null) }

    LaunchedEffect(data) {
        if (data != null) {
            lastData = data
            val durationMillis = when (data.visuals.duration) {
                SnackbarDuration.Short -> 4000L
                SnackbarDuration.Long -> 10_000L
                SnackbarDuration.Indefinite -> null
            }
            if (durationMillis != null) {
                delay(durationMillis.milliseconds)
                // Only dismiss when this is still the snackbar on screen,
                // so a manually-dismissed snackbar doesn't double-fire.
                if (hostState.currentSnackbarData == data) {
                    data.dismiss()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = data != null,
            enter = fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetY = { it },
                ),
            exit = fadeOut(animationSpec = tween(160)) +
                slideOutVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetY = { it },
                ),
        ) {
            val showing = lastData ?: data
            if (showing != null) {
                // Generous side gutters keep the pill compact; the bottom
                // padding floats it ~30% up from the bottom edge.
                FosterSnackbar(
                    data = showing,
                    style = style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                        .padding(horizontal = 40.dp)
                        .padding(top = 8.dp, bottom = maxHeight * 0.025f),
                )
            }
        }
    }
}

/**
 * Minimal [SnackbarData] stub so previews can render [FosterSnackbar] without
 * a real host.
 */
private class PreviewSnackbarData(
    override val visuals: SnackbarVisuals,
) : SnackbarData {
    override fun performAction() = Unit
    override fun dismiss() = Unit
}

@PreviewLightDark
@Composable
private fun FosterSnackbarStylesPreview() {
    FosterTheme {
        Column(
            modifier = Modifier
                .background(FosterTheme.colors.background.b0)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FosterSnackbar(
                data = PreviewSnackbarData(
                    FosterSnackbarVisuals(message = "Check-in logged"),
                ),
                style = FosterSnackbarStyle.Neutral,
            )
            FosterSnackbar(
                data = PreviewSnackbarData(
                    FosterSnackbarVisuals(message = "Contact saved"),
                ),
                style = FosterSnackbarStyle.Success,
            )
            FosterSnackbar(
                data = PreviewSnackbarData(
                    FosterSnackbarVisuals(message = "Couldn't check in. Please try again."),
                ),
                style = FosterSnackbarStyle.Error,
            )
            // Longer message to check wrapping stays centered.
            FosterSnackbar(
                data = PreviewSnackbarData(
                    FosterSnackbarVisuals(
                        message = "Name must be between 4 and 50 characters.",
                    ),
                ),
                style = FosterSnackbarStyle.Neutral,
            )
        }
    }
}