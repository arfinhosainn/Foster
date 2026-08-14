package app.usenekko.onboarding

import app.usenekko.onboarding.domain.OnboardingDraft
import app.usenekko.onboarding.domain.OnboardingDraftLocalDataSource
import app.usenekko.onboarding.domain.OnboardingProfileDataSource
import app.usenekko.onboarding.domain.OnboardingProfileError
import app.usenekko.onboarding.domain.OnboardingStep
import app.usenekko.onboarding.notification.NotificationAction
import app.usenekko.onboarding.notification.NotificationViewModel
import app.usenekko.onboarding.presentation.OnboardingDraftStore
import app.usenekko.shared.domain.EmptyResult
import app.usenekko.shared.domain.Result
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationViewModelTest {
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
}

private class NotificationDraftDataSource : OnboardingDraftLocalDataSource {
    override suspend fun getDraft(): OnboardingDraft = OnboardingDraft(currentStep = OnboardingStep.Notification)
    override suspend fun saveDraft(draft: OnboardingDraft) = Unit
    override suspend fun clearDraft() = Unit
}

private class FakeProfileDataSource : OnboardingProfileDataSource {
    override suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError> =
        Result.Success(Unit)

    override suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError> =
        Result.Success(OnboardingStep.Notification)

    override suspend fun ensureProfileExists(): EmptyResult<OnboardingProfileError> = Result.Success(Unit)
}