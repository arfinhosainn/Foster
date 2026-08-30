package app.usefoster.onboarding

import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.OnboardingDraftLocalDataSource
import app.usefoster.onboarding.domain.OnboardingDraftStorageError
import app.usefoster.onboarding.presentation.OnboardingDraftStore
import app.usefoster.shared.domain.EmptyResult
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingDraftStoreTest {

    @Test
    fun readFailureIsReportedAndUsesAnEmptyFallback() = runBlocking {
        val source = FakeDraftDataSource(
            readResult = Result.Error(OnboardingDraftStorageError.Read),
        )
        val store = OnboardingDraftStore(source)

        assertEquals(OnboardingDraftStorageError.Read, store.storageErrors.first())
        assertEquals(OnboardingDraft(), store.draft.value)
    }

    @Test
    fun writeFailureIsReportedWithoutDroppingTheInMemoryDraft() = runBlocking {
        val source = FakeDraftDataSource(
            saveResult = Result.Error(OnboardingDraftStorageError.Write),
        )
        val store = OnboardingDraftStore(source)

        store.update { it.copy(name = "Latest progress") }

        assertEquals(OnboardingDraftStorageError.Write, store.storageErrors.first())
        assertEquals("Latest progress", store.draft.value.name)
    }

    @Test
    fun clearFailureIsReportedAndRestoresTheCurrentDraft() = runBlocking {
        val source = FakeDraftDataSource(
            readResult = Result.Success(OnboardingDraft(name = "Saved progress")),
            clearResult = Result.Error(OnboardingDraftStorageError.Clear),
        )
        val store = OnboardingDraftStore(source)
        waitForDraft(store, "Saved progress")

        store.clear()

        assertEquals(OnboardingDraftStorageError.Clear, store.storageErrors.first())
        assertEquals("Saved progress", store.draft.value.name)
    }

    @Test
    fun corruptDraftIsReportedWithoutClearingThePersistedPayload() = runBlocking {
        val source = FakeDraftDataSource(
            readResult = Result.Error(OnboardingDraftStorageError.Corrupt),
        )
        val store = OnboardingDraftStore(source)

        assertEquals(OnboardingDraftStorageError.Corrupt, store.storageErrors.first())
        assertEquals(0, source.clearCalls)
        assertEquals(OnboardingDraft(), store.draft.value)
    }

    @Test
    fun rapidUpdatesLeaveTheNewestDraftPersisted() = runBlocking {
        val source = FakeDraftDataSource()
        val store = OnboardingDraftStore(source)

        store.update { it.copy(name = "First draft") }
        store.update { it.copy(name = "Latest draft") }

        while (source.savedDraft?.name != "Latest draft") {
            delay(1)
        }

        assertEquals("Latest draft", source.savedDraft?.name)
    }

    private suspend fun waitForDraft(store: OnboardingDraftStore, name: String) {
        while (store.draft.value.name != name) {
            delay(1)
        }
    }
}

private class FakeDraftDataSource(
    private val readResult: Result<OnboardingDraft, OnboardingDraftStorageError> =
        Result.Success(OnboardingDraft()),
    private val saveResult: EmptyResult<OnboardingDraftStorageError> = Result.Success(Unit),
    private val clearResult: EmptyResult<OnboardingDraftStorageError> = Result.Success(Unit),
) : OnboardingDraftLocalDataSource {
    var clearCalls = 0
    var savedDraft: OnboardingDraft? = null

    override suspend fun getDraft(): Result<OnboardingDraft, OnboardingDraftStorageError> = readResult

    override suspend fun saveDraft(draft: OnboardingDraft): EmptyResult<OnboardingDraftStorageError> {
        if (saveResult is Result.Success) savedDraft = draft
        return saveResult
    }

    override suspend fun clearDraft(): EmptyResult<OnboardingDraftStorageError> {
        clearCalls++
        return clearResult
    }
}