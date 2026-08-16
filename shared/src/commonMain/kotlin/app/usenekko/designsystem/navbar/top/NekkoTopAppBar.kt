package app.usenekko.designsystem.navbar.top

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.AudienceOption
import app.usenekko.designsystem.buttons.NekkoDropDownButton
import app.usenekko.designsystem.shapes.MonogramAvatar
import app.usenekko.theme.NekkoTheme
import nekko.shared.generated.resources.Res
import nekko.shared.generated.resources.ic_acquaintance
import nekko.shared.generated.resources.ic_crown
import nekko.shared.generated.resources.ic_dropdown
import nekko.shared.generated.resources.ic_family
import nekko.shared.generated.resources.ic_friends
import nekko.shared.generated.resources.ic_group
import nekko.shared.generated.resources.ic_person
import org.jetbrains.compose.resources.vectorResource

@Composable
fun NekkoTopBar(
    audienceOptions: List<AudienceOption>,
    selectedAudience: AudienceOption,
    onAudienceSelect: (AudienceOption) -> Unit,
    userName: String,
    onAvatarClick: () -> Unit,
    onPremiumClick: () -> Unit,
    avatarContent: @Composable () -> Unit = {
        MonogramAvatar(name = userName)
    },
    modifier: Modifier = Modifier,
) = CenterAlignedTopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = NekkoTheme.colors.background.b0,
        scrolledContainerColor = NekkoTheme.colors.background.b0,
    ),
    title = {
        NekkoDropDownButton(
            options = audienceOptions,
            selected = selectedAudience,
            onSelect = onAudienceSelect,
            chevron = Res.drawable.ic_dropdown,
        )
    },
    navigationIcon = {
        Box(
            modifier = Modifier
                .padding(start = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick),
            ) {
                avatarContent()
            }
        }
    },
    actions = {
        IconButton(onClick = onPremiumClick) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_crown),
                contentDescription = "Premium",
                tint = Color.Unspecified,
            )
        }
    },
)


@PreviewLightDark
@Composable
private fun PreviewNekkoTopBar() = NekkoTheme {
    val options = listOf(
        AudienceOption("Everyone", Res.drawable.ic_group),
        AudienceOption("Family", Res.drawable.ic_family),
        AudienceOption("Friends", Res.drawable.ic_friends),
        AudienceOption("Acquaintance", Res.drawable.ic_acquaintance),
        AudienceOption("Others", Res.drawable.ic_person),
    )
    var selected by remember { mutableStateOf(options.first()) }

    Box(
        Modifier
            .fillMaxWidth()
            .background(NekkoTheme.colors.background.b0),
    ) {
        NekkoTopBar(
            audienceOptions = options,
            selectedAudience = selected,
            onAudienceSelect = { selected = it },
            userName = "Jane Bell",
            onAvatarClick = {},
            onPremiumClick = {},
        )
    }
}