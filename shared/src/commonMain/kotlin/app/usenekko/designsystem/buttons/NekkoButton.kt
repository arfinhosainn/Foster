package app.usenekko.designsystem.buttons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme

@Composable
fun NekkoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = NekkoTheme.colors.background.onBackground,
        contentColor = NekkoTheme.colors.background.b1,
        disabledContainerColor = NekkoTheme.colors.gray.quaternary,
        disabledContentColor = NekkoTheme.colors.background.b0,
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 35.dp, vertical = 15.dp),
    textStyle: TextStyle = NekkoTheme.typography.heading3Bold,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = NekkoTheme.colors.background.b1,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                leadingIcon()
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = if (loading) "Please wait" else text,
            style = textStyle,
            maxLines = 1
        )

        if (trailingIcon != null && !loading) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                trailingIcon()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NekkoButtonPreview() {
    NekkoTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NekkoButton(text = "Continue with Apple", onClick = {}, shape = CircleShape)
            Spacer(modifier = Modifier.height(12.dp))
            NekkoButton(text = "Next", onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            NekkoButton(text = "Select Avatar", onClick = {}, enabled = true)
            Spacer(modifier = Modifier.height(12.dp))
            NekkoButton(text = "Join the room in progress", onClick = {}, loading = false)
            Spacer(modifier = Modifier.height(16.dp))
            NekkoButton(
                text = "Join the room in progress",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
