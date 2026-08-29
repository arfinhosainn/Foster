package app.usenekko.onboarding

import app.usenekko.onboarding.contact.ContactAction
import app.usenekko.onboarding.contact.ContactEvent
import app.usenekko.onboarding.contact.ContactViewModel
import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingDraftStorageError
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun importFailureEmitsAUserFacingEvent() = runTest {
        val viewModel = ContactViewModel(OnboardingDraftStore(ContactInMemoryDraftDataSource()))
        val event = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.events.first()
        }

        viewModel.onAction(ContactAction.ImportFailed)

        assertEquals(ContactEvent.ImportFailed, event.await())
    }
}

private class ContactInMemoryDraftDataSource : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> =
        Result.Success(OnboardingDraft())

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)
}