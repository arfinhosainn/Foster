package app.usefoster.home.presentation.paywall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import app.usefoster.designsystem.snackbar.FosterSnackbarHost
import app.usefoster.designsystem.snackbar.FosterSnackbarStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usefoster.designsystem.buttons.FosterButton
import app.usefoster.home.di.rememberPaywallViewModel
import app.usefoster.shared.subscription.BillingPeriod
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.fire
import foster.home.generated.resources.ic_brainstorm
import foster.home.generated.resources.ic_calendergradient
import foster.home.generated.resources.ic_circlecheckmark
import foster.home.generated.resources.ic_close
import foster.home.generated.resources.ic_contacts
import foster.home.generated.resources.ic_groupgradient
import foster.home.generated.resources.ic_headphonegradient
import foster.home.generated.resources.ic_insightgradient
import foster.home.generated.resources.ic_reminder
import foster.home.generated.resources.ic_support
import foster.home.generated.resources.ic_treeleft
import foster.home.generated.resources.ic_treeright
import foster.home.generated.resources.gradients
import foster.home.generated.resources.ic_fire
import foster.home.generated.resources.paywall_gradient
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import foster.home.generated.resources.cd_close
import foster.home.generated.resources.cd_selected
import foster.home.generated.resources.paywall_annual_plan
import foster.home.generated.resources.paywall_billing_note
import foster.home.generated.resources.paywall_monthly_plan
import foster.home.generated.resources.paywall_restore_cta
import foster.home.generated.resources.paywall_restoring
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString

@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    onSubscribed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberPaywallViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PaywallEvent.Subscribed -> onSubscribed()
                is PaywallEvent.ShowError -> snackbarHostState.showSnackbar(getString(event.message))
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(FosterTheme.colors.background.b0),
    ) {
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
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50))
                        .background(PaywallCloseBackground),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.cd_close),
                        tint = FosterTheme.colors.gray.secondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().height(480.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = PaywallGreen)
                }
            } else {
                FeatureCard()
                Spacer(Modifier.height(24.dp))
                PlanList(
                    selectedPeriod = state.selectedPeriod,
                    monthlyPrice = state.offering?.monthly?.priceString,
                    annualPrice = state.offering?.annual?.priceString,
                    onSelect = { viewModel.onAction(PaywallAction.SelectPeriod(it)) },
                )
                Spacer(Modifier.height(48.dp))
                FosterButton(
                    text = state.ctaText,
                    onClick = { viewModel.onAction(PaywallAction.Purchase) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    enabled = !state.isPurchasing && state.selectedPackage != null,
                    loading = state.isPurchasing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FosterTheme.colors.background.onBackground,
                        contentColor = PaywallBackground,
                        disabledContainerColor = PaywallWhite.copy(alpha = 0.5f),
                        disabledContentColor = PaywallBackground.copy(alpha = 0.5f),
                    ),
                    contentPadding = PaddingValues(0.dp),
                    textStyle = FosterTheme.typography.heading3Bold.copy(
                        color = PaywallBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(32.dp))
                TextButton(
                    onClick = { viewModel.onAction(PaywallAction.Restore) },
                    enabled = !state.isRestoring,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isRestoring) stringResource(Res.string.paywall_restoring) else stringResource(Res.string.paywall_restore_cta),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = PaywallWhite,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(Res.string.paywall_billing_note),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    color = PaywallMuted,
                )
            }
        }

        FosterSnackbarHost(
            hostState = snackbarHostState,
            style = FosterSnackbarStyle.Error,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

private data class PaywallFeature(
    val title: String,
    val icon: DrawableResource,
    val comingSoon: Boolean = false,
)

private val paywallFeatures = listOf(
    PaywallFeature("Unlimited Contacts", Res.drawable.ic_groupgradient),
    PaywallFeature("Smart Reminder", Res.drawable.ic_calendergradient, comingSoon = true),
    PaywallFeature("Relationship Insight", Res.drawable.ic_insightgradient, comingSoon = true),
    PaywallFeature("Premium Support", Res.drawable.ic_headphonegradient),
)

@Composable
private fun FeatureCard() {
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(40.dp)),
    ) {
        Image(
            painter = painterResource(Res.drawable.gradients),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxHeight()
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Foster",
                    color = PaywallWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(Color(0x665A774C))
                        .border(1.dp, Color(0x8897A98D), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "UNLIMITED",
                        color = PaywallWhite,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                paywallFeatures.forEach { feature ->
                    PaywallFeatureRow(feature)
                }
            }
        }
    }
}

@Composable
private fun PaywallFeatureRow(feature: PaywallFeature) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(feature.icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(15.dp))
        Text(
            feature.title,
            color = PaywallWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (feature.comingSoon) {
            Text(
                "COMING SOON",
                color = FosterTheme.colors.text.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(FosterTheme.colors.fill.secondary)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 24.dp, height = 8.dp)
                .clip(RoundedCornerShape(50)).background(PaywallWhite),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50))
                .background(PaywallIndicatorInactive),
        )
    }
}

@Composable
private fun PlanList(
    selectedPeriod: BillingPeriod,
    monthlyPrice: String?,
    annualPrice: String?,
    onSelect: (BillingPeriod) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlanCard(
            title = stringResource(Res.string.paywall_annual_plan),
            price = annualPrice ?: "$35.00",
            periodLabel = "year",
            originalPrice = "\$99.99",
            discount = "40% OFF",
            isSelected = selectedPeriod == BillingPeriod.ANNUAL,
            onClick = { onSelect(BillingPeriod.ANNUAL) },
        )
        PlanCard(
            title = stringResource(Res.string.paywall_monthly_plan),
            price = monthlyPrice ?: "39.99",
            periodLabel = "month",
            isSelected = selectedPeriod == BillingPeriod.MONTHLY,
            onClick = { onSelect(BillingPeriod.MONTHLY) },
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    periodLabel: String,
    originalPrice: String? = null,
    discount: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FosterTheme.colors.background.b1)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(PaywallGreen, PaywallYellow)),
                    shape = shape,
                ) else Modifier,
            )
            .clickable(onClick = onClick)
            .wrapContentHeight()
            .padding(horizontal = 20.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PaywallMuted,
                    )
                    discount?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            color = FosterTheme.colors.yellow.stroke,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(40))
                                .background(PaywallYellow)
                                .padding(horizontal = 6.dp, vertical = 0.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    originalPrice?.let {
                        Text(
                            it,
                            color = PaywallMuted,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        price,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = PaywallWhite
                    )
                    Text(
                        "/ $periodLabel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = PaywallWhite
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_circlecheckmark),
                    contentDescription = stringResource(Res.string.cd_selected),
                    tint = FosterTheme.colors.gray.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private val PaywallBackground = Color(0xFF080809)
private val PaywallWhite = Color(0xFFF8F8F8)
private val PaywallMuted = Color(0xFF8F8E98)
private val PaywallGreen = Color(0xFF22C55E)
private val PaywallGreenYellow = Color(0xFFB7D82D)
private val PaywallYellow = Color(0xFFFACC15)
private val PaywallCloseBackground = Color.Transparent
private val PaywallPlanBackground = Color(0xFF19191D)
private val PaywallComingSoonText = Color(0xFFB4B2BC)
private val PaywallComingSoonBackground = Color(0x332C3A34)
private val PaywallIndicatorInactive = Color(0xFF404047)


@PreviewLightDark
@Composable
fun PreviewPaywallScreen() {
    FosterTheme {
        PaywallScreen(onBack = {}, onSubscribed = {})
    }
}