package app.usefoster.onboarding.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal fun formatPhoneNumberDisplay(digits: String): String = buildString {
    digits.forEachIndexed { index, char ->
        when (index) {
            0 -> append('(')
            3 -> append(") - ")
            6 -> append(' ')
        }
        append(char)
    }
}

internal object PhoneGroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatPhoneNumberDisplay(digits)

        fun rawToFormatted(raw: Int): Int {
            var extra = 0
            if (raw >= 1) extra += 1 // "("
            if (raw >= 4) extra += 4 // ") - "
            if (raw >= 7) extra += 1 // " "
            return (raw + extra).coerceAtMost(formatted.length)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = rawToFormatted(offset)
            override fun transformedToOriginal(offset: Int): Int {
                for (raw in 0..digits.length) {
                    if (rawToFormatted(raw) >= offset) return raw
                }
                return digits.length
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
