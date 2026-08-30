package app.usefoster.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Semantic token shapes
// ---------------------------------------------------------------------------

@Immutable
data class PaletteColor(
    val default: Color,
    val hover: Color,
    val active: Color,
    val stroke: Color,
    val fill: Color,
)

@Immutable
data class TextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val quaternary: Color,
)

@Immutable
data class GrayColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val quaternary: Color,
)

@Immutable
data class OverlayColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val quaternary: Color,
)

@Immutable
data class StrokeColors(
    val primary: Color,
    val secondary: Color,
)

@Immutable
data class BackgroundColors(
    val b0: Color,
    val b1: Color,
    val b2: Color,
    val b3: Color,
    val onBackground: Color,
)

@Immutable
data class ExtendedColors(
    val blue: PaletteColor,
    val green: PaletteColor,
    val yellow: PaletteColor,
    val orange: PaletteColor,
    val red: PaletteColor,
    val violet: PaletteColor,
    val text: TextColors,
    val gray: GrayColors,
    val fill: OverlayColors,
    val stroke: StrokeColors,
    val background: BackgroundColors,
)

private fun palette(default: Color, hover: Color, active: Color, stroke: Color, fill: Color) =
    PaletteColor(default, hover, active, stroke, fill)

// ---------------------------------------------------------------------------
// Light / dark token sets
// ---------------------------------------------------------------------------

val LightExtendedColors = ExtendedColors(
    blue = palette(Blue, BlueHover, BlueActive, BlueStroke, BlueFill),
    green = palette(Green, GreenHover, GreenActive, GreenStroke, GreenFill),
    yellow = palette(Yellow, YellowHover, YellowActive, YellowStroke, YellowFill),
    orange = palette(Orange, OrangeHover, OrangeActive, OrangeStroke, OrangeFill),
    red = palette(Red, RedHover, RedActive, RedStroke, RedFill),
    violet = palette(Violet, VioletHover, VioletActive, VioletStroke, VioletFill),
    text = TextColors(TextPrimaryLight, TextSecondaryLight, TextTertiaryLight, TextQuaternaryLight),
    gray = GrayColors(GrayPrimaryLight, GraySecondaryLight, GrayTertiaryLight, GrayQuaternaryLight),
    fill = OverlayColors(
        primary = InkLight.copy(alpha = 0.16f),
        secondary = InkLight.copy(alpha = 0.08f),
        tertiary = InkLight.copy(alpha = 0.04f),
        quaternary = InkLight.copy(alpha = 0.02f),
    ),
    stroke = StrokeColors(
        primary = InkLight.copy(alpha = 0.12f),
        secondary = InkLight.copy(alpha = 0.06f),
    ),
    background = BackgroundColors(BackgroundB0Light, BackgroundB1Light, BackgroundB2Light, BackgroundB3Light, onBackground = TextPrimaryLight),
)

val DarkExtendedColors = ExtendedColors(
    blue = palette(Blue, BlueHover, BlueActive, BlueStroke, BlueFill),
    green = palette(Green, GreenHover, GreenActive, GreenStroke, GreenFill),
    yellow = palette(Yellow, YellowHover, YellowActive, YellowStroke, YellowFill),
    orange = palette(Orange, OrangeHover, OrangeActive, OrangeStroke, OrangeFill),
    red = palette(Red, RedHover, RedActive, RedStroke, RedFill),
    violet = palette(Violet, VioletHover, VioletActive, VioletStroke, VioletFill),
    text = TextColors(TextPrimaryDark, TextSecondaryDark, TextTertiaryDark, TextQuaternaryDark),
    gray = GrayColors(GrayPrimaryDark, GraySecondaryDark, GrayTertiaryDark, GrayQuaternaryDark),
    fill = OverlayColors(
        primary = InkDark.copy(alpha = 0.16f),
        secondary = InkDark.copy(alpha = 0.08f),
        tertiary = InkDark.copy(alpha = 0.04f),
        quaternary = InkDark.copy(alpha = 0.02f),
    ),
    stroke = StrokeColors(
        primary = InkDark.copy(alpha = 0.12f),
        secondary = InkDark.copy(alpha = 0.06f),
    ),
    background = BackgroundColors(BackgroundB0Dark, BackgroundB1Dark, BackgroundB2Dark, BackgroundB3Dark, onBackground = TextPrimaryDark),
)

// ---------------------------------------------------------------------------
// CompositionLocal + accessor
// ---------------------------------------------------------------------------

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided. Wrap your content in FosterTheme { ... }.")
}

/**
 * The RESOLVED dark-mode flag for the current FosterTheme — honors the in-app
 * Appearance setting first, falling back to the system theme. Read this instead
 * of isSystemInDarkTheme() when styling custom surfaces (navbar, sheets, ...).
 */
val LocalFosterIsDark = staticCompositionLocalOf { false }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

object FosterTheme {
    val colors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}

// ---------------------------------------------------------------------------
// Material 3 ColorScheme mapping
// ---------------------------------------------------------------------------

private fun lightMaterialColorScheme(colors: ExtendedColors): ColorScheme = lightColorScheme(
    primary = colors.blue.default,
    onPrimary = Color.White,
    primaryContainer = colors.blue.fill,
    onPrimaryContainer = colors.blue.active,
    secondary = colors.violet.default,
    onSecondary = Color.White,
    secondaryContainer = colors.violet.fill,
    onSecondaryContainer = colors.violet.active,
    tertiary = colors.green.default,
    onTertiary = Color.White,
    tertiaryContainer = colors.green.fill,
    onTertiaryContainer = colors.green.active,
    error = colors.red.default,
    onError = Color.White,
    errorContainer = colors.red.fill,
    onErrorContainer = colors.red.active,
    background = colors.background.b1,
    onBackground = colors.text.primary,
    surface = colors.background.b1,
    onSurface = colors.text.primary,
    surfaceVariant = colors.background.b0,
    onSurfaceVariant = colors.text.secondary,
    outline = colors.gray.tertiary,
    outlineVariant = colors.gray.quaternary,
    scrim = Color.Black,
)

private fun darkMaterialColorScheme(colors: ExtendedColors): ColorScheme = darkColorScheme(
    primary = colors.blue.default,
    onPrimary = Color.White,
    primaryContainer = colors.blue.active,
    onPrimaryContainer = colors.blue.stroke,
    secondary = colors.violet.default,
    onSecondary = Color.White,
    secondaryContainer = colors.violet.active,
    onSecondaryContainer = colors.violet.stroke,
    tertiary = colors.green.default,
    onTertiary = Color.White,
    tertiaryContainer = colors.green.active,
    onTertiaryContainer = colors.green.stroke,
    error = colors.red.default,
    onError = Color.White,
    errorContainer = colors.red.active,
    onErrorContainer = colors.red.stroke,
    background = colors.background.b1,
    onBackground = colors.text.primary,
    surface = colors.background.b1,
    onSurface = colors.text.primary,
    surfaceVariant = colors.background.b2,
    onSurfaceVariant = colors.text.secondary,
    outline = colors.gray.tertiary,
    outlineVariant = colors.gray.quaternary,
    scrim = Color.Black,
)

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun FosterTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val themeStore = LocalThemeStore.current
    val selectedMode = themeStore?.mode?.collectAsState()?.value ?: AppThemeMode.SYSTEM
    val isDark = darkTheme ?: resolveDarkTheme(selectedMode, isSystemInDarkTheme())
    val extended = if (isDark) DarkExtendedColors else LightExtendedColors
    val materialColors = if (isDark) darkMaterialColorScheme(extended) else lightMaterialColorScheme(extended)
    val appTypo = appTypography()

    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalFosterIsDark provides isDark,
        LocalAppTypography provides appTypo,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = materialTypography(appTypo),
        ) {
            CompositionLocalProvider(
                LocalIndication provides ripple(),
                content = content,
            )
        }
    }
}
