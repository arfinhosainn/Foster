# Voqal Onboarding Architecture Reference

> Reference from the Voqal project for future implementation in Nekko.

## Directory Structure

```
shared/src/commonMain/kotlin/app/voqal/com/feature/onboarding/
├── data/
│   ├── DataStoreOnboardingDraftDataSource.kt    # Local draft persistence (DataStore)
│   ├── SupabaseOnboardingAuthDataSource.kt      # Auth network layer (Supabase)
│   ├── SupabaseOnboardingProfileDataSource.kt   # Profile network layer (Supabase)
│   └── dto/
│       └── ProfileDtos.kt                       # All serializable DTOs
├── di/
│   └── OnboardingPresentationModule.kt          # Koin DI module
├── domain/
│   ├── OnboardingDraft.kt                        # Core draft data class
│   ├── OnboardingDraftLocalDataSource.kt         # Interface for local storage
│   ├── OnboardingAuthDataSource.kt               # Interface for auth ops
│   ├── OnboardingAuthError.kt                    # Auth error enum
│   ├── OnboardingProfileDataSource.kt            # Interface for profile ops
│   ├── OnboardingProfileError.kt                 # Profile error enum
│   ├── OnboardingProfileErrorMessages.kt         # Error-to-message mapper
│   └── validation/
│       └── ValidatePassword.kt                   # Password validation
├── presentation/
│   ├── OnboardingDraftStore.kt                   # Shared state holder
│   ├── components/
│   │   ├── BackButton.kt
│   │   └── ValidationHint.kt
│   ├── navigation/
│   │   ├── OnboardingRoutes.kt                   # Route sealed interface
│   │   └── OnboardingGraph.kt                    # Nav graph builder
│   ├── email/        # Step 1
│   ├── password/     # Step 2
│   ├── fullname/     # Step 3
│   ├── username/     # Step 4
│   ├── photo/        # Step 5
│   ├── language/     # Step 6
│   └── interest/     # Step 7 (final)
```

## MVI Pattern (per screen)

Each screen has exactly 5 files:

| File | Role |
|---|---|
| `*State.kt` | Immutable data class for UI state |
| `*Action.kt` | Sealed interface of user intents |
| `*Event.kt` | One-shot side effects (navigation, snackbar) |
| `*ViewModel.kt` | Processes Actions → updates State + emits Events |
| `*Screen.kt` | Compose UI (Root + Screen composable) |

### Flow

```
User Gesture → Screen calls onAction(Action.Xxx)
    → ViewModel processes:
        1. Updates State via _state.update { }
        2. Persists to OnboardingDraftStore
        3. Optionally calls DataSource (network)
        4. Optionally sends Event via _events.send(...)
    → Screen collects state + observes events
```

### Example

```kotlin
// Action
sealed interface EmailAction {
    data class OnEmailChange(val email: String) : EmailAction
    data object OnContinueClick : EmailAction
}

// State
data class EmailState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = email.trim().let { it.isNotEmpty() && it.contains("@") }
}

// Event
sealed interface EmailEvent {
    data class NavigateToNext(val isNewUser: Boolean) : EmailEvent
    data class ShowSnackbar(val message: String) : EmailEvent
}

// ViewModel
class EmailViewModel(
    private val onboardingDraftStore: OnboardingDraftStore,
    private val onboardingAuthDataSource: OnboardingAuthDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow(EmailState(email = onboardingDraftStore.email))
    val state = _state.asStateFlow()
    private val _events = Channel<EmailEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: EmailAction) {
        when (action) {
            is EmailAction.OnEmailChange -> _state.update { it.copy(email = action.email) }
            is EmailAction.OnContinueClick -> checkEmail()
        }
    }
}
```

## Offline Draft Persistence

### OnboardingDraft Model

```kotlin
data class OnboardingDraft(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val profilePhotoUri: String? = null,
    val selectedLanguageId: String? = null,
    val selectedInterestIds: Set<String> = emptySet(),
    val currentStep: Int = 1,
    val lastUpdatedAtMillis: Long = 0L,
)
```

### DataStore Implementation

```kotlin
class DataStoreOnboardingDraftDataSource(
    private val dataStore: DataStore<Preferences>,
) : OnboardingDraftLocalDataSource {

    private object Keys {
        val EmailKey = stringPreferencesKey("onboarding_email")
        val FirstNameKey = stringPreferencesKey("onboarding_first_name")
        // ... other keys namespaced with "onboarding_"
    }

    override suspend fun getDraft(): OnboardingDraft {
        return dataStore.data.map { prefs ->
            OnboardingDraft(
                email = prefs[Keys.EmailKey] ?: "",
                // ... read all fields
            )
        }.first()
    }

    override suspend fun saveDraft(draft: OnboardingDraft) {
        dataStore.edit { prefs ->
            prefs[Keys.EmailKey] = draft.email
            // ... write all fields
        }
    }

    override suspend fun clearDraft() {
        dataStore.edit { it.clear() }
    }
}
```

### OnboardingDraftStore (State Holder)

```kotlin
class OnboardingDraftStore(
    private val localDataSource: OnboardingDraftLocalDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft = _draft.asStateFlow()

    init {
        scope.launch { _draft.value = localDataSource.getDraft() }  // Restore on creation
    }

    fun updateEmail(email: String) {
        updateDraft(currentStep = 1) { it.copy(email = email) }
    }

    private fun updateDraft(currentStep: Int, transform: (OnboardingDraft) -> OnboardingDraft) {
        _draft.update { transform(it).copy(currentStep = currentStep) }
        scope.launch { localDataSource.saveDraft(_draft.value) }  // Write-through
    }

    fun clear() { /* clear DataStore */ }
}
```

## Batch Sync at Final Step

Steps 1-6: **local only** (DataStore).
Step 7: **batch sync** to Supabase in sequence:

```kotlin
private fun submitSelectedInterests() {
    viewModelScope.launch {
        onboardingProfileDataSource.ensureProfileExists()
        onboardingProfileDataSource.updateFullName(draft.firstName, draft.lastName)
        onboardingProfileDataSource.updateUsername(draft.username)
        onboardingProfileDataSource.uploadAvatar(profilePhotoBytes)
        onboardingProfileDataSource.updateLanguage(draft.selectedLanguageId)
        onboardingProfileDataSource.completeOnboarding(selectedIds)
    }
}
```

Each step fails independently with specific error messages via `ShowSnackbar` events.

## Server-Side Step Tracking

`profiles.onboarding_step` integer column:

| Step | Value |
|---|---|
| Profile created | 2 |
| Full name updated | 3 |
| Username updated | 4 |
| Photo uploaded | 5 |
| Language updated | 6 |
| Onboarding completed | 7 |

`SplashViewModel` checks this to route:
```kotlin
when (val result = profileDataSource.getOnboardingStep()) {
    is Result.Success -> {
        if (result.data != null && result.data >= 7) SplashEvent.Authenticated
        else SplashEvent.NotAuthenticated
    }
}
```

## Dependency Injection (Koin)

```kotlin
val onboardingPresentationModule = module {
    single { createOnboardingDraftDataStore() }
    single<SupabaseConfig> { VoqalSupabaseConfig.current }
    single<SupabaseClient> { SupabaseClientFactory.create(get()) }

    singleOf(::DataStoreOnboardingDraftDataSource) { bind<OnboardingDraftLocalDataSource>() }
    singleOf(::SupabaseOnboardingAuthDataSource)   { bind<OnboardingAuthDataSource>() }
    singleOf(::SupabaseOnboardingProfileDataSource) { bind<OnboardingProfileDataSource>() }

    singleOf(::OnboardingDraftStore)

    viewModel { EmailViewModel(get(), get()) }
    viewModel { PasswordViewModel(get(), get(), get()) }
    viewModel { FullNameViewModel(get(), get(), get()) }
    viewModel { UsernameViewModel(get(), get()) }
    viewModel { AddPhotoViewModel(get(), get(), get()) }
    viewModel { LanguageViewModel(get(), get()) }
    viewModel { ChooseInterestsViewModel(get(), get()) }
}
```

## Error Handling

```kotlin
enum class OnboardingProfileError : Error {
    NotConfigured, NotAuthenticated, UsernameTaken, Network, Unknown
}

fun OnboardingProfileError.toUserMessage(): String = when (this) {
    NotConfigured -> "Supabase is not configured yet"
    NotAuthenticated -> "Sign in again to continue"
    UsernameTaken -> "That username is already taken"
    Network -> "Check your connection and try again"
    Unknown -> "Something went wrong. Please try again"
}
```

Network exceptions mapped via:
```kotlin
private fun Throwable.toOnboardingProfileError(): OnboardingProfileError = when (this) {
    is HttpRequestException, is HttpRequestTimeoutException -> Network
    is RestException -> if (isUsernameConflict()) UsernameTaken else Unknown
    else -> Unknown
}
```

## Core Result Type

```kotlin
sealed interface Result<out D, out E> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E>(val error: E) : Result<Nothing, E>
}
typealias EmptyResult<E> = Result<Unit, E>
```

## Key Takeaways for Nekko

1. **MVI per screen** — State/Action/Event/ViewModel/Screen (5 files each)
2. **DataStore for draft persistence** — write-through on every change, restore on startup
3. **Batch sync at final step** — avoid per-step API calls, sync everything at the end
4. **Server-side step tracking** — integer column in profiles table for resume support
5. **Koin DI** — `singleOf` + `bind` for data sources, `viewModel { }` for ViewModels
6. **Result type** — typed errors, mapped to user-friendly messages
7. **No use-case layer** — ViewModels call DataSource interfaces directly
8. **No Room for onboarding** — DataStore Preferences only (key-value)
