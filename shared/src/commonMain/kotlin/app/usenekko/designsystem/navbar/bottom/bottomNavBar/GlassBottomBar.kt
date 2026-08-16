package app.usenekko.designsystem.navbar.bottom.bottomNavBar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.GlassIconButton
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import nekko.shared.generated.resources.Res
import nekko.shared.generated.resources.ic_contact
import nekko.shared.generated.resources.ic_home
import nekko.shared.generated.resources.ic_leaf
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

private const val BarHeight = 68
private const val IndicatorSize = 54
private const val CellWidth = 56
private const val PillInnerPadding = 8 // NEW — start/end breathing room inside the pill

data class NavDestination(val label: String, val icon: DrawableResource)

private val Destinations = listOf(
    NavDestination("Home", Res.drawable.ic_home),
    NavDestination("Grow", Res.drawable.ic_leaf),
)

@Composable
fun GlassBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    liquidState: LiquidState,          // ← passed in, not remembered here
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
            liquidState = liquidState
        )
        GlassIconButton(
            icon = Res.drawable.ic_contact,
            contentDescription = "Invite someone",
            onClick = onAddClick,
            liquidState = liquidState,
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
    liquidState: LiquidState,
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
            liquidState = liquidState,
        )
        Spacer(Modifier.weight(1f))
        GlassIconButton(
            icon = Res.drawable.ic_contact,
            contentDescription = "Invite someone",
            onClick = onAddClick,
            liquidState = liquidState,
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
    liquidState: LiquidState,
    modifier: Modifier = Modifier,
) {
    val cellWidth = CellWidth.dp
    val innerPadding = PillInnerPadding.dp

    val glassTint = if (isSystemInDarkTheme()) NekkoTheme.colors.fill.secondary
    else Color.White.copy(alpha = 0.45f)

    Box(
        modifier
            .height(BarHeight.dp)
            .width(cellWidth * destinations.size + innerPadding * 2)
            .liquid(liquidState) {
                frost = 5.dp
                shape = CircleShape
                refraction = 0.18f     // subtle, this look is soft not lensy
                curve = 0.30f
                edge = 0.06f           // that faint light rim on the top-left
                tint = glassTint  // dark glass, not white
                saturation = 1.15f
                dispersion = 0f        // no rainbow fringe in your ref
            }
            .clip(CircleShape),
    ) {
        val indicatorX by animateDpAsState(
            targetValue = innerPadding + cellWidth * selectedIndex + (cellWidth - IndicatorSize.dp) / 2, // ← shifted by innerPadding
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
                .padding(horizontal = innerPadding), // ← the actual fix
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
    liquidState: LiquidState,
    modifier: Modifier = Modifier,
) {
    val cellHeight = CellWidth.dp
    val innerPadding = PillInnerPadding.dp
    val glassTint = if (isSystemInDarkTheme()) NekkoTheme.colors.fill.secondary
    else Color.White.copy(alpha = 0.45f)

    Box(
        modifier
            .width(BarHeight.dp)
            .height(cellHeight * destinations.size + innerPadding * 2)
            .liquid(liquidState) {
                frost = 5.dp
                shape = RoundedCornerShape(34.dp)
                refraction = 0.18f
                curve = 0.30f
                edge = 0.06f
                tint = glassTint
                saturation = 1.15f
                dispersion = 0f
            }
            .clip(RoundedCornerShape(34.dp)),
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

    val dark = isSystemInDarkTheme()   // swap for NekkoTheme's own isDark flag if you have one

    val iconTint by animateColorAsState(
        targetValue = when {
            dark && selected -> NekkoTheme.colors.text.primary
            dark -> NekkoTheme.colors.gray.secondary
            selected -> NekkoTheme.colors.gray.secondary
            else -> Color.Black
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
            contentDescription = destination.label,
            tint = iconTint,
            modifier = Modifier.size(25.dp).scale(squeeze),
        )
    }
}

@Composable
fun AmbientGlow(liquidState: LiquidState, modifier: Modifier = Modifier) = Box(
    modifier
        .fillMaxSize()
        .background(NekkoTheme.colors.background.b0)
        .liquefiable(liquidState),
) {

}

@PreviewLightDark
@Composable
private fun PreviewGlassBottomBar() = NekkoTheme {
    Box(Modifier.fillMaxSize()) {
        val liquidState = rememberLiquidState()
        AmbientGlow(liquidState)              // SOURCE, sibling
        GlassBottomNavBar(
            // EFFECT, sibling
            selectedIndex = 1,
            onItemSelected = {},
            onAddClick = {},
            liquidState = liquidState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun glassTint(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Color.Black.copy(alpha = 0.55f)
    else Color.White.copy(alpha = 0.45f)   // frosted light glass
}

