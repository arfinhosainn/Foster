package app.usenekko.home

import app.usenekko.home.presentation.paywall.PaywallEvent
import app.usenekko.home.presentation.paywall.PaywallViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    @Test
    fun loadingErrorIsEmittedAsOneTimeErrorEvent() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = PaywallViewModel(FakeSubscriptionRepository())
            advanceUntilIdle()

            assertEquals(
                PaywallEvent.ShowError("Couldn't load plans. Please try again."),
                viewModel.events.first(),
            )
        } finally {
            Dispatchers.resetMain()
        }
    }
}