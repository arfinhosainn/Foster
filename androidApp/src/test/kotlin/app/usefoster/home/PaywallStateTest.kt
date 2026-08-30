package app.usefoster.home

import app.usefoster.home.presentation.paywall.PaywallState
import app.usefoster.shared.subscription.BillingPeriod
import app.usefoster.shared.subscription.PaywallOffering
import app.usefoster.shared.subscription.PaywallPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaywallStateTest {

    @Test
    fun annualPlanIsSelectedByDefaultAndTrialTextUsesTheSelectedPackage() {
        val annual = packageFor(BillingPeriod.ANNUAL, hasFreeTrial = true, trialString = "7-day free trial")
        val monthly = packageFor(BillingPeriod.MONTHLY, hasFreeTrial = false)
        val state = PaywallState(offering = PaywallOffering(monthly = monthly, annual = annual))

        assertEquals(BillingPeriod.ANNUAL, state.selectedPeriod)
        assertEquals(annual, state.selectedPackage)
        assertEquals("Continue with 7-day free trial", state.ctaText)
    }

    @Test
    fun switchingToMonthlyPlanUpdatesTheSelectedPackageAndCta() {
        val annual = packageFor(BillingPeriod.ANNUAL, hasFreeTrial = true, trialString = "7-day free trial")
        val monthly = packageFor(BillingPeriod.MONTHLY, hasFreeTrial = false)
        val state = PaywallState(
            offering = PaywallOffering(monthly = monthly, annual = annual),
            selectedPeriod = BillingPeriod.MONTHLY,
        )

        assertEquals(monthly, state.selectedPackage)
        assertEquals("Subscribe", state.ctaText)
    }

    @Test
    fun missingPlanFallsBackToSubscribeWithoutSelectingAPackage() {
        val state = PaywallState(offering = PaywallOffering(monthly = null, annual = null))

        assertNull(state.selectedPackage)
        assertEquals("Subscribe", state.ctaText)
    }

    private fun packageFor(
        period: BillingPeriod,
        hasFreeTrial: Boolean,
        trialString: String? = null,
    ) = PaywallPackage(
        identifier = period.name.lowercase(),
        period = period,
        priceString = "$39.99",
        periodString = period.name.lowercase(),
        hasFreeTrial = hasFreeTrial,
        trialString = trialString,
    )
}