package app.usenekko.home.presentation.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.home.di.rememberPaywallViewModel
import app.usenekko.shared.subscription.BillingPeriod
import app.usenekko.theme.NekkoTheme

@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    onSubscribed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberPaywallViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is PaywallEvent.Subscribed) onSubscribed()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NekkoTheme.colors.text.primary)
                }
            }
        },
        containerColor = NekkoTheme.colors.background.b0,
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NekkoTheme.colors.fill.primary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                Text("Foster Unlimited", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = NekkoTheme.colors.text.primary)
                Spacer(Modifier.height(8.dp))
                Text("Unlock the full Nekko experience", fontSize = 16.sp,
                    color = NekkoTheme.colors.text.secondary)
                Spacer(Modifier.height(28.dp))
                BenefitList()
                Spacer(Modifier.height(28.dp))
                PlanToggle(
                    selectedPeriod = state.selectedPeriod,
                    monthlyPrice = state.offering?.monthly?.priceString,
                    annualPrice = state.offering?.annual?.priceString,
                    onSelect = { viewModel.onAction(PaywallAction.SelectPeriod(it)) },
                )
                Spacer(Modifier.height(24.dp))
                NekkoButton(
                    text = state.ctaText,
                    onClick = { viewModel.onAction(PaywallAction.Purchase) },
                    enabled = !state.isPurchasing && state.selectedPackage != null,
                    loading = state.isPurchasing,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { viewModel.onAction(PaywallAction.Restore) },
                    enabled = !state.isRestoring, modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isRestoring) "Restoring…" else "Restore Purchase",
                        fontSize = 14.sp, color = NekkoTheme.colors.text.secondary)
                }
                state.error?.let { errMsg ->
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(NekkoTheme.colors.fill.secondary).padding(12.dp)) {
                        Text(errMsg, fontSize = 13.sp, color = NekkoTheme.colors.text.secondary)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private data class Benefit(val title: String, val subtitle: String, val comingSoon: Boolean = false)

private val benefits = listOf(
    Benefit("Unlimited Contacts", "Add as many contacts as you want"),
    Benefit("Brainstorming", "Generate unlimited conversation ideas"),
    Benefit("Smart Reminders", "Coming soon", comingSoon = true),
    Benefit("Relationship Insights", "Coming soon", comingSoon = true),
)

@Composable
private fun BenefitList() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        benefits.forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50))
                        .background(NekkoTheme.colors.fill.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        tint = NekkoTheme.colors.text.primary, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        benefit.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (benefit.comingSoon) NekkoTheme.colors.text.tertiary
                            else NekkoTheme.colors.text.primary,
                    )
                    Text(
                        benefit.subtitle,
                        fontSize = 13.sp,
                        color = NekkoTheme.colors.text.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanToggle(
    selectedPeriod: BillingPeriod,
    monthlyPrice: String?,
    annualPrice: String?,
    onSelect: (BillingPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlanCard(
            title = "Monthly",
            price = monthlyPrice ?: "—",
            periodLabel = "per month",
            isSelected = selectedPeriod == BillingPeriod.MONTHLY,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(BillingPeriod.MONTHLY) },
        )
        PlanCard(
            title = "Annual",
            price = annualPrice ?: "—",
            periodLabel = "per year",
            isSelected = selectedPeriod == BillingPeriod.ANNUAL,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(BillingPeriod.ANNUAL) },
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    periodLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) NekkoTheme.colors.fill.primary
        else NekkoTheme.colors.fill.secondary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor,
                shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.primary)
        Spacer(Modifier.height(8.dp))
        Text(price, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = NekkoTheme.colors.text.primary)
        Text(periodLabel, fontSize = 12.sp, color = NekkoTheme.colors.text.tertiary)
    }
}


@PreviewLightDark
@Composable
fun PreviewPaywallScreen() {
    NekkoTheme {
        PaywallScreen(onBack = {}, onSubscribed = {})
    }
}