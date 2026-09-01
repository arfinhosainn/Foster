package app.usefoster.home.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.cd_back
import foster.home.generated.resources.cd_select_year
import foster.home.generated.resources.history_title
import foster.home.generated.resources.ic_back
import foster.home.generated.resources.ic_dropdown
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Top bar for the check-in history screen: back button, centered "History"
 * title and a trailing year selector pill (e.g. 2026) opening a dropdown.
 */
@Composable
fun HistoryTopBar(
    onBack: () -> Unit,
    years: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = CenterAlignedTopAppBar(
    modifier = modifier.padding(horizontal = 20.dp),
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = FosterTheme.colors.background.b0,
        scrolledContainerColor = FosterTheme.colors.background.b0,
    ),
    title = {
        Text(
            stringResource(Res.string.history_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = FosterTheme.colors.text.primary,
            textAlign = TextAlign.Center,
        )
    },
    navigationIcon = {
        FilledIconButton(
            modifier = Modifier.size(40.dp),
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = FosterTheme.colors.background.b1,
                contentColor = FosterTheme.colors.text.primary,
            ),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_back),
                contentDescription = stringResource(Res.string.cd_back),
                modifier = Modifier.size(20.dp),
            )
        }
    },
    actions = {
        HistoryYearDropdown(
            years = years,
            selectedYear = selectedYear,
            onYearSelected = onYearSelected,
        )
    },
)

@Composable
private fun HistoryYearDropdown(
    years: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedYear.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = FosterTheme.colors.text.primary,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = vectorResource(Res.drawable.ic_dropdown),
                contentDescription = stringResource(Res.string.cd_select_year),
                tint = FosterTheme.colors.gray.primary,
                modifier = Modifier.size(16.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = year.toString(),
                            fontWeight = if (year == selectedYear) FontWeight.SemiBold else FontWeight.Normal,
                            color = FosterTheme.colors.text.primary,
                        )
                    },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewHistoryTopBar() = FosterTheme {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    ) {
        HistoryTopBar(
            onBack = {},
            years = listOf(2026, 2025, 2024),
            selectedYear = 2026,
            onYearSelected = {},
        )
    }
}
