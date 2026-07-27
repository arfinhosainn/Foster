package app.usenekko.onboarding.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme

@Composable
fun VerificationCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isConfirmed: Boolean = false,
    isLoading: Boolean = false,
    onEditRequested: (() -> Unit)? = null,
    onDone: (String) -> Unit = {},
) {
    NekkoStepField(
        isConfirmed = isConfirmed,
        isLoading = isLoading,
        modifier = modifier,
        onClick = if (isConfirmed) onEditRequested else null,
    ) {
        if (isConfirmed) {
            Text(
                text = code,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NekkoTheme.colors.text.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (code.isEmpty()) {
                    Text(
                        text = "$length digit verification code",
                        fontSize = 16.sp,
                        color = NekkoTheme.colors.text.tertiary,
                    )
                }
                BasicTextField(
                    value = code,
                    onValueChange = { raw -> onCodeChange(raw.filter(Char::isDigit).take(length)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = NekkoTheme.typography.bodyMedium.fontFamily,
                        color = NekkoTheme.colors.text.primary,
                    ),
                    cursorBrush = SolidColor(NekkoTheme.colors.text.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone(code) },
                    ),
                )
            }
        }
    }
}
