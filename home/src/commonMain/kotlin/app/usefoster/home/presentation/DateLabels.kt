package app.usefoster.home.presentation

import androidx.compose.runtime.Composable
import foster.home.generated.resources.Res
import foster.home.generated.resources.date_month_apr
import foster.home.generated.resources.date_month_aug
import foster.home.generated.resources.date_month_dec
import foster.home.generated.resources.date_month_feb
import foster.home.generated.resources.date_month_jan
import foster.home.generated.resources.date_month_jul
import foster.home.generated.resources.date_month_jun
import foster.home.generated.resources.date_month_mar
import foster.home.generated.resources.date_month_may
import foster.home.generated.resources.date_month_nov
import foster.home.generated.resources.date_month_oct
import foster.home.generated.resources.date_month_sep
import org.jetbrains.compose.resources.stringResource

/**
 * Month abbreviations ("Jan".."Dec") resolved in the current locale.
 * Index 0 = January, matching `kotlinx.datetime.Month.ordinal`, so date
 * formatters can do `months[date.month.ordinal]`.
 */
@Composable
fun rememberMonthAbbreviations(): List<String> = listOf(
    stringResource(Res.string.date_month_jan),
    stringResource(Res.string.date_month_feb),
    stringResource(Res.string.date_month_mar),
    stringResource(Res.string.date_month_apr),
    stringResource(Res.string.date_month_may),
    stringResource(Res.string.date_month_jun),
    stringResource(Res.string.date_month_jul),
    stringResource(Res.string.date_month_aug),
    stringResource(Res.string.date_month_sep),
    stringResource(Res.string.date_month_oct),
    stringResource(Res.string.date_month_nov),
    stringResource(Res.string.date_month_dec),
)