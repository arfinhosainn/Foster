package app.usefoster.onboarding.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.onboarding.utils.PhoneGroupingTransformation
import app.usefoster.onboarding.utils.formatPhoneNumberDisplay
import app.usefoster.theme.FosterTheme
import foster.onboarding.generated.resources.Res
import foster.onboarding.generated.resources.phone_enter_number
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhoneNumberField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    countryCode: String = "+60",
    isConfirmed: Boolean = false,
    maxDigits: Int = 10,
    onEditRequested: (() -> Unit)? = null,
    onDone: (fullNumber: String) -> Unit = {},
) {
    FosterStepField(
        isConfirmed = isConfirmed,
        modifier = modifier,
        onClick = if (isConfirmed) onEditRequested else null,
    ) {
        if (isConfirmed) {
            Text(
                text = "$countryCode ${formatPhoneNumberDisplay(phoneNumber)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FosterTheme.colors.text.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = countryCode,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = FosterTheme.colors.text.tertiary,
                )
                Box(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    if (phoneNumber.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.phone_enter_number),
                            fontSize = 16.sp,
                            color = FosterTheme.colors.text.tertiary,
                        )
                    }
                    BasicTextField(
                        value = phoneNumber,
                        onValueChange = { raw -> onPhoneNumberChange(raw.filter(Char::isDigit).take(maxDigits)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FosterTheme.typography.bodyMedium.fontFamily,
                            color = FosterTheme.colors.text.primary,
                        ),
                        cursorBrush = SolidColor(FosterTheme.colors.text.primary),
                        visualTransformation = PhoneGroupingTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { onDone("$countryCode$phoneNumber") },
                        ),
                    )
                }
            }
        }
    }
}
