package app.usenekko.designsystem.navbar.bottom.bottomNavBar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.usenekko.theme.LocalNekkoIsDark
import app.usenekko.theme.NekkoTheme
import nekko.shared.generated.resources.Res
import nekko.shared.generated.resources.cd_invite_someone
import nekko.shared.generated.resources.ic_contact
import nekko.shared.generated.resources.ic_home
import nekko.shared.generated.resources.ic_leaf
import nekko.shared.generated.resources.nav_grow
import nekko.shared.generated.resources.nav_home
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private const val BarHeight = 68
private const val IndicatorSize = 54
private const val CellWidth = 56
private const val PillInnerPadding = 8 // start/end breathing room inside the pill

// Solid navbar background (frosted-glass / Liquid removed).
// Dark: keep #18181B. Light: white.
// Follows the IN-APP appearance setting via LocalNekkoIsDark, not the OS theme.
@Composable
private fun navBarBackground(): Color =
    if (LocalNekkoIsDark.current) Color(0xFF18181B) else Color.White

/**
 * The "shine" on the side corners: a thin rim light that is brightest along the
 * left and right edges and fades to nothing toward the middle. Painted as a 1dp
 * border with a horizontal gradient so it hugs whatever [shape] the surface uses.
 */
private fun Modifier.topEdgeHighlight(shape: Shape): Modifier = this.border(
    width = 1.dp,
    brush = Brush.horizontalGradient(
        0.00f to Color.White.copy(alpha = 0.18f), // hot left edge
        0.30f to Color.White.copy(alpha = 0.05f),
        0.50f to Color.Transparent,               // clear through the middle
        0.70f to Color.White.copy(alpha = 0.05f),
        1.00f to Color.White.copy(alpha = 0.18f), // hot right edge
    ),
    shape = shape,
)

data class NavDestination(val labelRes: StringResource, val icon: DrawableResource)

private val Destinations = listOf(
    NavDestination(Res.string.nav_home, Res.drawable.ic_home),
    NavDestination(Res.string.nav_grow, Res.drawable.ic_leaf),
)

@Composable
fun GlassBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        // Centers the (pill + circle) cluster as a group, instead of letting
        // the pill stretch to fill the row and drag the icons apart.
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavPill(
            destinations = Destinations,
            selectedIndex = selectedIndex,
            onSelect = onItemSelected,
        )
        SolidIconButton(
            icon = Res.drawable.ic_contact,
            contentDescription = stringResource(Res.string.cd_invite_someone),
            onClick = onAddClick,
            size = BarHeight.dp,
            iconSize = 26.dp,
        )
    }
}

@Composable
fun GlassNavigationRail(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .width(88.dp)
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavRailPill(
            destinations = Destinations,
            selectedIndex = selectedIndex,
            onSelect = onItemSelected,
        )
        Spacer(Modifier.weight(1f))
        SolidIconButton(
            icon = Res.drawable.ic_contact,
            contentDescription = stringResource(Res.string.cd_invite_someone),
            onClick = onAddClick,
            size = BarHeight.dp,
            iconSize = 26.dp,
        )
    }
}

@Composable
private fun NavPill(
    destinations: List<NavDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellWidth = CellWidth.dp
    val innerPadding = PillInnerPadding.dp

    Box(
        modifier
            .height(BarHeight.dp)
            .width(cellWidth * destinations.size + innerPadding * 2)
            .clip(CircleShape)
            .background(navBarBackground())
            .topEdgeHighlight(CircleShape), // ← the shine
    ) {
        val indicatorX by animateDpAsState(
            targetValue = innerPadding + cellWidth * selectedIndex + (cellWidth - IndicatorSize.dp) / 2,
            animationSpec = spring(dampingRatio = 0.68f, stiffness = 340f),
            label = "indicatorX",
        )

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = indicatorX)
                .size(IndicatorSize.dp)
                .background(NekkoTheme.colors.fill.tertiary, CircleShape),
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = innerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                NavCell(
                    destination = destination,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.width(cellWidth).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun NavRailPill(
    destinations: List<NavDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellHeight = CellWidth.dp
    val innerPadding = PillInnerPadding.dp
    val pillShape = RoundedCornerShape(34.dp)

    Box(
        modifier
            .width(BarHeight.dp)
            .height(cellHeight * destinations.size + innerPadding * 2)
            .clip(pillShape)
            .background(navBarBackground())
            .topEdgeHighlight(pillShape), // ← the shine
    ) {
        val indicatorY by animateDpAsState(
            targetValue = innerPadding + cellHeight * selectedIndex + (cellHeight - IndicatorSize.dp) / 2,
            animationSpec = spring(dampingRatio = 0.68f, stiffness = 340f),
            label = "indicatorY",
        )

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = indicatorY)
                .size(IndicatorSize.dp)
                .background(NekkoTheme.colors.fill.tertiary, CircleShape),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            destinations.forEachIndexed { index, destination ->
                NavCell(
                    destination = destination,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.width(cellHeight).height(cellHeight),
                )
            }
        }
    }
}

@Composable
private fun NavCell(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val dark = LocalNekkoIsDark.current // in-app appearance setting, not the OS theme

    val iconTint by animateColorAsState(
        targetValue = when {
            dark && selected -> NekkoTheme.colors.text.primary
            dark -> NekkoTheme.colors.gray.secondary
            selected -> Color.Black
            else -> NekkoTheme.colors.gray.secondary
        },
        animationSpec = tween(240),
        label = "iconTint",
    )
    val squeeze by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "squeeze",
    )

    Box(
        modifier.selectable(
            selected = selected,
            interactionSource = interaction,
            indication = null,
            role = Role.Tab,
            onClick = {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(destination.icon),
            contentDescription = stringResource(destination.labelRes),
            tint = iconTint,
            modifier = Modifier.size(25.dp).scale(squeeze),
        )
    }
}

/**
 * Solid replacement for the old glass icon button. Same dark surface as the
 * pill, plus the matching top-edge shine.
 */
@Composable
private fun SolidIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squeeze by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "iconSqueeze",
    )

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(navBarBackground())
            .topEdgeHighlight(CircleShape) // ← the shine
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(icon),
            contentDescription = contentDescription,
            tint = NekkoTheme.colors.gray.secondary,
            modifier = Modifier.size(iconSize).scale(squeeze),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewGlassBottomBar() = NekkoTheme {
    Box(
        Modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
    ) {
        GlassBottomNavBar(
            selectedIndex = 1,
            onItemSelected = {},
            onAddClick = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
