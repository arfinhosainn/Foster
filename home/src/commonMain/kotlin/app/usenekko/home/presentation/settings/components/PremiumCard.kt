package app.usenekko.home.presentation.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.gradients
import nekko.home.generated.resources.ic_crown
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.action_upgrade
import nekko.home.generated.resources.premium_add_unlimited
import nekko.home.generated.resources.premium_foster
import nekko.home.generated.resources.premium_unlimited
import org.jetbrains.compose.resources.stringResource

@Composable
fun PremiumCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(32.dp)
    val upgradeShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .clip(cardShape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {

        Image(
            painter = painterResource(Res.drawable.gradients),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.premium_foster),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = stringResource(Res.string.premium_unlimited),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(
                                horizontal = 4.5.dp,
                                vertical = 5.5.dp,
                            ),
                    )
                }

                Text(
                    text = stringResource(Res.string.premium_add_unlimited),
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(16.dp))

            Row(
                modifier = Modifier
                    .clip(upgradeShape)
                    .background(Color.White)
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_crown),
                    contentDescription = null,
                    tint = Color(0xFFFFC400),
                    modifier = Modifier.size(16.dp),
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(Res.string.action_upgrade),
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PremiumCardPreview() {
    NekkoTheme {

        PremiumCard(onClick = {})
    }
}