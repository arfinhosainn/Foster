package app.usenekko.home.presentation.settings.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.GlassIconButton
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_appearance
import nekko.home.generated.resources.ic_contacts
import nekko.home.generated.resources.ic_greenprofile
import nekko.home.generated.resources.ic_groups
import nekko.home.generated.resources.ic_notification
import nekko.home.generated.resources.ic_support
import nekko.home.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.DrawableResource
import nekko.home.generated.resources.settings_account
import nekko.home.generated.resources.settings_appearance
import nekko.home.generated.resources.settings_contacts
import nekko.home.generated.resources.settings_delete_account
import nekko.home.generated.resources.settings_groups
import nekko.home.generated.resources.settings_notification
import nekko.home.generated.resources.settings_support
import org.jetbrains.compose.resources.stringResource

sealed interface SettingsRow {

    data class Item(
        val icon: DrawableResource,
        val title: String,
        val subtitle: String? = null,
        val trailing: String? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit
    ) : SettingsRow

    data class Destructive(
        val icon: DrawableResource,
        val title: String,
        val onClick: () -> Unit
    ) : SettingsRow
}

@Composable
fun SettingsGroup(
    rows: List<SettingsRow>,
    liquidState: LiquidState,
    modifier: Modifier = Modifier
) {

    val liquidScope = rememberLiquidState()

    Surface(
        modifier = modifier.liquefiable(liquidState),
        color = NekkoTheme.colors.background.b1,
        shape = RoundedCornerShape(40.dp)
    ) {

        Column {

            rows.forEachIndexed { index, row ->

                when (row) {

                    is SettingsRow.Item ->
                        SettingsItem(
                            item = row,
                            liquidState = liquidState
                        )

                    is SettingsRow.Destructive ->
                        SettingsDeleteItem(
                            item = row,
                            liquidState = liquidState
                        )
                }

                if (index != rows.lastIndex) {

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 92.dp),
                        thickness = .5.dp,
                        color = NekkoTheme.colors.stroke.secondary
                    )
                }
            }
        }
    }
}


@Composable
private fun SettingsItem(
    item: SettingsRow.Item,
    liquidState: LiquidState
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { item.onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        GlassIconButton(
            icon = item.icon,
            contentDescription = item.title,
            onClick = item.onClick,
            liquidState = liquidState,   // ← replace both TODO()s
        )

        Spacer(Modifier.width(18.dp))

        Text(
            text = item.title,
            style = NekkoTheme.typography.heading4Semibold,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f)
        )

        item.trailing?.let {

            Text(
                text = it,
                color = NekkoTheme.colors.text.secondary
            )

            Spacer(Modifier.width(12.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = NekkoTheme.colors.text.tertiary
        )
    }
}


@Composable
private fun SettingsDeleteItem(
    item: SettingsRow.Destructive,
    liquidState: LiquidState
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { item.onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        GlassIconButton(
            icon = item.icon,
            contentDescription = item.title,
            onClick = item.onClick,
            liquidState = liquidState,   // ← was missing
        )

        Spacer(Modifier.width(18.dp))

        Text(
            text = item.title,
            color = Color(0xFFFF4B4B),
            style = NekkoTheme.typography.heading4Semibold
        )
    }
}


@PreviewLightDark
@Composable
fun PreviewSettingsGroup() {
    NekkoTheme {

        val liquidState = rememberLiquidState()

        SettingsGroup(
            liquidState = liquidState,
            rows = listOf(

                SettingsRow.Item(
                    icon = Res.drawable.ic_greenprofile,
                    title = stringResource(Res.string.settings_account)
                ) { },

                SettingsRow.Item(
                    icon = Res.drawable.ic_appearance,
                    title = stringResource(Res.string.settings_appearance)
                ) { },

                SettingsRow.Item(
                    icon = Res.drawable.ic_notification,
                    title = stringResource(Res.string.settings_notification),
                    trailing = "Off"
                ) { },

                SettingsRow.Item(
                    icon = Res.drawable.ic_contacts,
                    title = stringResource(Res.string.settings_contacts)
                ) { },

                SettingsRow.Item(
                    icon = Res.drawable.ic_groups,
                    title = stringResource(Res.string.settings_groups)
                ) { },

                SettingsRow.Item(
                    icon = Res.drawable.ic_support,
                    title = stringResource(Res.string.settings_support)
                ) { }
            )
        )

        Spacer(Modifier.height(20.dp))

        SettingsGroup(
            liquidState = liquidState,
            rows = listOf(
                SettingsRow.Destructive(
                    icon = Res.drawable.ic_trashbin,
                    title = stringResource(Res.string.settings_delete_account)
                ) { }
            )
        )
    }
}