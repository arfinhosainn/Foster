package app.usefoster.onboarding.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import app.usefoster.theme.appTypography
import app.usefoster.theme.extendedColors


@Composable
fun TermsAndPrivacyNotice(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.extendedColors
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = colors.text.primary,
            fontWeight = FontWeight.Bold,
        ),
    )

    val text = buildAnnotatedString {
        append("By continuing, you accept our ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "terms",
                styles = linkStyles,
                linkInteractionListener = { onTermsClick() },
            ),
        ) {
            append("Terms &\n Conditions")
        }
        append(" and ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacy",
                styles = linkStyles,
                linkInteractionListener = { onPrivacyClick() },
            ),
        ) {
            append("Privacy Policy")
        }
        append(".")
    }

    Text(
        text = text,
        style = MaterialTheme.appTypography.captionL.copy(
            color = colors.text.secondary,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier,
    )
}

