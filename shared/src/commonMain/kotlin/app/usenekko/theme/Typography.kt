package app.usenekko.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import nekko.shared.generated.resources.Res

import nekko.shared.generated.resources.sf_pro_rounded
import nekko.shared.generated.resources.sf_pro_rounded_medium
import nekko.shared.generated.resources.sf_pro_rounded_semibold
import nekko.shared.generated.resources.sf_pro_rounded_bold

@Composable
internal fun sfProRoundedFontFamily(): FontFamily = FontFamily(
    Font(Res.font.sf_pro_rounded, FontWeight.Normal),
    Font(Res.font.sf_pro_rounded_medium, FontWeight.Medium),
    Font(Res.font.sf_pro_rounded_semibold, FontWeight.SemiBold),
    Font(Res.font.sf_pro_rounded_bold, FontWeight.Bold),
)

private fun textStyle(fontFamily: FontFamily, size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

@Immutable
data class AppTypography(
    val display: TextStyle,
    val displayBold: TextStyle,
    val heading1: TextStyle,
    val heading1Bold: TextStyle,
    val heading2: TextStyle,
    val heading2Bold: TextStyle,
    val heading3: TextStyle,
    val heading3Bold: TextStyle,
    val heading4: TextStyle,
    val heading4Semibold: TextStyle,
    val heading5: TextStyle,
    val heading5Semibold: TextStyle,
    val body: TextStyle,
    val bodyMedium: TextStyle,
    val footnote: TextStyle,
    val footnoteMedium: TextStyle,
    val captionL: TextStyle,
    val captionLSemibold: TextStyle,
    val captionM: TextStyle,
    val captionMSemibold: TextStyle,
    val captionS: TextStyle,
    val captionSBold: TextStyle,
)

@Composable
fun appTypography(): AppTypography {
    val sharedFamily = sfProRoundedFontFamily()

    fun heading(size: Int, lineHeight: Int, weight: FontWeight) = textStyle(sharedFamily, size, lineHeight, weight)
    fun body(size: Int, lineHeight: Int, weight: FontWeight) = textStyle(sharedFamily, size, lineHeight, weight)

    return AppTypography(
        display = heading(34, 41, FontWeight.Normal),
        displayBold = heading(34, 41, FontWeight.Bold),
        heading1 = heading(28, 36, FontWeight.Normal),
        heading1Bold = heading(28, 36, FontWeight.SemiBold),
        heading2 = heading(24, 31, FontWeight.Normal),
        heading2Bold = heading(24, 31, FontWeight.Bold),
        heading3 = heading(20, 26, FontWeight.Normal),
        heading3Bold = heading(20, 26, FontWeight.Bold),
        heading4 = heading(17, 21, FontWeight.Medium),
        heading4Semibold = heading(17, 21, FontWeight.SemiBold),
        heading5 = heading(14, 18, FontWeight.Normal),
        heading5Semibold = heading(14, 18, FontWeight.SemiBold),
        body = body(14, 20, FontWeight.Normal),
        bodyMedium = body(14, 20, FontWeight.Medium),
        footnote = body(13, 20, FontWeight.Normal),
        footnoteMedium = body(13, 20, FontWeight.Medium),
        captionL = body(12, 17, FontWeight.Normal),
        captionLSemibold = body(12, 17, FontWeight.SemiBold),
        captionM = body(11, 15, FontWeight.Normal),
        captionMSemibold = body(11, 15, FontWeight.SemiBold),
        captionS = body(10, 14, FontWeight.Normal),
        captionSBold = body(10, 14, FontWeight.Bold),
    )
}

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No AppTypography provided. Wrap your content in NekkoTheme { ... }.")
}

val MaterialTheme.appTypography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTypography.current

fun materialTypography(typography: AppTypography): Typography = Typography(
    displayLarge = typography.displayBold,
    displayMedium = typography.display,
    displaySmall = typography.heading1Bold,
    headlineLarge = typography.heading1,
    headlineMedium = typography.heading2Bold,
    headlineSmall = typography.heading2,
    titleLarge = typography.heading3Bold,
    titleMedium = typography.heading3,
    titleSmall = typography.heading4Semibold,
    bodyLarge = typography.heading4,
    bodyMedium = typography.body,
    bodySmall = typography.footnote,
    labelLarge = typography.bodyMedium,
    labelMedium = typography.footnoteMedium,
    labelSmall = typography.captionL,
)
