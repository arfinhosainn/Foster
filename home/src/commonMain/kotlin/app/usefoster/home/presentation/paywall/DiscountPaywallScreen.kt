package app.usefoster.home.presentation.paywall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usefoster.home.di.rememberDiscountPaywallViewModel
import app.usefoster.theme.FosterTheme
import kotlinx.coroutines.delay
import foster.home.generated.resources.Res
import foster.home.generated.resources.discountgradient
import foster.home.generated.resources.discounts
import foster.home.generated.resources.gradientss
import foster.home.generated.resources.grass
import foster.home.generated.resources.ic_arrowforward
import foster.home.generated.resources.ic_treeleft
import foster.home.generated.resources.ic_treeright
import foster.home.generated.resources.left_flower
import foster.home.generated.resources.right_flower
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import foster.home.generated.resources.discount_billing_note
import foster.home.generated.resources.discount_claim_cta
import foster.home.generated.resources.discount_expires_in
import foster.home.generated.resources.discount_for_annual
import foster.home.generated.resources.discount_lowest_price
import foster.home.generated.resources.discount_one_time_offer
import foster.home.generated.resources.discount_processing
import foster.home.generated.resources.paywall_restore_cta
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString

/**
 * Real pricing worked backward from the 60% discount:
 * original annual $48.99 -> 60% off -> $19.60/year -> $1.63/month.
 */
private const val ORIGINAL_ANNUAL_PRICE = "$48.99"
private const val DISCOUNTED_ANNUAL_PRICE = "$19.60"
private const val DISCOUNTED_MONTHLY_PRICE = "$1.63"

/** Fallback countdown (12h 29m) while no real deadline is available (preview). */
private const val DEFAULT_OFFER_DURATION_SECONDS = 12L * 3600 + 29L * 60

/** Pale sage tint applied to the monochrome -60% art and laurel branches. */
private val DiscountArtTint = Color(0xFFD9E2C9).copy(alpha = 0.85f)
private val DiscountMuted = Color(0xFF9A9E93)

// Shared paywall palette (mirrors the constants in PaywallScreen.kt).
private val PaywallBackground = Color(0xFF080809)
private val PaywallWhite = Color(0xFFF8F8F8)
private val PaywallMuted = Color(0xFF8F8E98)
private val PaywallGreen = Color(0xFF22C55E)

@Composable
fun DiscountPaywallScreen(
    onSubscribed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DiscountPaywallViewModel = rememberDiscountPaywallViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val offerExpiresAtMillis by viewModel.offerExpiresAtMillis.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DiscountPaywallEvent.Subscribed -> onSubscribed()
                is DiscountPaywallEvent.ShowError -> snackbarHostState.showSnackbar(getString(event.message))
            }
        }
    }

    // Countdown ticks against the REAL persisted deadline (stamped once at the
    // first impression), so leaving and re-entering the screen never resets it.
    var remainingSeconds by remember {
        mutableLongStateOf(DEFAULT_OFFER_DURATION_SECONDS)
    }

    LaunchedEffect(offerExpiresAtMillis) {
        val deadline = offerExpiresAtMillis
        while (true) {
            remainingSeconds = if (deadline == null) {
                DEFAULT_OFFER_DURATION_SECONDS
            } else {
                val now = Clock.System.now().toEpochMilliseconds()
                ((deadline - now).coerceAtLeast(0L)) / 1000L
            }
            delay(1_000.milliseconds)
        }
    }

    val hours = (remainingSeconds / 3600).toString().padStart(2, '0')
    val minutes = ((remainingSeconds % 3600) / 60).toString().padStart(2, '0')
    val seconds = (remainingSeconds % 60).toString().padStart(2, '0')
    val countdownText = "$hours : $minutes : $seconds"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PaywallBackground),
    ) {
        // Tree decorations at the bottom corners.
        Image(
            painter = painterResource(Res.drawable.ic_treeleft),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomStart),
        )
        Image(
            painter = painterResource(Res.drawable.ic_treeright),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // Hero offer card.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp)),
            ) {

                Image(
                    painterResource(Res.drawable.grass), contentDescription = null,
                    modifier = modifier.align(Alignment.TopCenter)
                )

                Image(
                    painter = painterResource(Res.drawable.discountgradient),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize().align(Alignment.TopCenter),
                )

                // Card content on top of the gradient.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 54.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.discounts),
                        contentDescription = "60% discount",
                        colorFilter = ColorFilter.tint(DiscountArtTint),
                        modifier = Modifier.fillMaxWidth(0.55f),
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.discount_one_time_offer),
                        color = PaywallWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.discount_expires_in),
                        color = DiscountMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.left_flower),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(DiscountArtTint),
                            modifier = Modifier.size(width = 26.dp, height = 46.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = countdownText,
                            color = PaywallWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(2.dp))
                        Image(
                            painter = painterResource(Res.drawable.right_flower),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(DiscountArtTint),
                            modifier = Modifier.size(width = 26.dp, height = 46.dp),
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "$DISCOUNTED_MONTHLY_PRICE / month",
                            color = PaywallWhite.copy(alpha = 0.75f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(11.dp))
                    Text(
                        text = stringResource(Res.string.discount_lowest_price),
                        color = PaywallGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(60.dp))

                    // Price anchor: strikethrough original -> bold discounted.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ORIGINAL_ANNUAL_PRICE,
                            color = DiscountMuted,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            vectorResource(Res.drawable.ic_arrowforward),
                            contentDescription = null,
                            tint = PaywallWhite
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = DISCOUNTED_ANNUAL_PRICE,
                            color = PaywallWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.discount_for_annual),
                        color = DiscountMuted,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(54.dp))

            Button(
                onClick = { viewModel.onAction(DiscountPaywallAction.Purchase) },
                enabled = !state.isPurchasing && !state.isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaywallWhite,
                    contentColor = PaywallBackground,
                ),
            ) {
                Text(
                    text = if (state.isPurchasing) stringResource(Res.string.discount_processing) else stringResource(Res.string.discount_claim_cta),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(32.dp))

            TextButton(
                onClick = { viewModel.onAction(DiscountPaywallAction.Restore) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isRestoring) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = PaywallGreen,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.paywall_restore_cta),
                        color = PaywallWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.discount_billing_note),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = PaywallMuted,
                fontSize = 14.sp,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@PreviewLightDark
@Composable
private fun DiscountPaywallPreview() {
    FosterTheme {
        DiscountPaywallScreen()
    }
}

