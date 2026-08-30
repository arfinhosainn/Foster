package app.usefoster.onboarding

import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.onboarding.domain.OnboardingProfileDataSource
import app.usefoster.onboarding.domain.OnboardingProfileError
import app.usefoster.onboarding.domain.OnboardingStep
import app.usefoster.onboarding.notification.NotificationAction
import app.usefoster.onboarding.notification.NotificationEvent
import app.usefoster.onboarding.notification.NotificationViewModel
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun permissionStateControlsNotificationEnabledState() {
        val viewModel = NotificationViewModel(
            draftStore = OnboardingDraftStore(NotificationDraftDataSource()),
            profileDataSource = FakeProfileDataSource(),
        )

        viewModel.onAction(NotificationAction.PermissionStateChanged(enabled = true))
        assertTrue(viewModel.state.value.isNotificationEnabled)

        viewModel.onAction(NotificationAction.PermissionStateChanged(enabled = false))
        assertFalse(viewModel.state.value.isNotificationEnabled)
    }

    @Test
    fun failedSubmissionStaysAtNotificationAndCanBeRetried() = runTest {
        val draftStore = OnboardingDraftStore(NotificationDraftDataSource())
        val profileDataSource = FakeProfileDataSource(
            submitResult = Result.Error(OnboardingProfileError.Network),
        )
        val viewModel = NotificationViewModel(draftStore, profileDataSource)
        val errorEvent = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.events.first()
        }

        viewModel.onAction(NotificationAction.SkipClicked)

        assertEquals(
            NotificationEvent.ShowError(OnboardingProfileError.Network),
            errorEvent.await(),
        )
        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals(OnboardingStep.Notification, draftStore.draft.value.currentStep)
        assertEquals(OnboardingStep.Notification, profileDataSource.submittedDraft?.currentStep)

        profileDataSource.submitResult = Result.Success(Unit)
        val navigationEvent = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.events.first()
        }

        viewModel.onAction(NotificationAction.PermissionResult(granted = true))

        assertEquals(NotificationEvent.NavigateToMainApp, navigationEvent.await())
        assertEquals(OnboardingDraft(), draftStore.draft.value)
        assertEquals(OnboardingStep.Notification, profileDataSource.submittedDraft?.currentStep)
    }
}

private class NotificationDraftDataSource : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> =
        Result.Success(OnboardingDraft(currentStep = OnboardingStep.Notification))

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> =
        Result.Success(Unit)
}

private class FakeProfileDataSource(
    var submitResult: EmptyResult<OnboardingProfileError> = Result.Success(Unit),
) : OnboardingProfileDataSource {
    var submittedDraft: OnboardingDraft? = null

    override suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError> {
        submittedDraft = draft
        return submitResult
    }

    override suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError> =
        Result.Success(OnboardingStep.Notification)

    override suspend fun ensureProfileExists(): EmptyResult<OnboardingProfileError> = Result.Success(Unit)
}