# Voqal Onboarding Architecture Reference

> Reference from the Voqal project for future implementation in Foster.
>
> Important: the Voqal screen names below are **reference-only**. Do not implement Voqal screens such as email, password, username, language, or interests in Foster unless the Foster product explicitly adds those screens later. Foster must use the actual onboarding route list from `shared/src/commonMain/kotlin/app/usefoster/navigation/Screen.kt` and `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/OnboardingApp.kt`.

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

## Key Takeaways for Foster

1. **MVI per screen** — State/Action/Event/ViewModel/Screen (5 files each)
2. **DataStore for draft persistence** — write-through on every change, restore on startup
3. **Batch sync at final step** — avoid per-step API calls, sync everything at the end
4. **Server-side step tracking** — integer column in profiles table for resume support
5. **Koin DI** — `singleOf` + `bind` for data sources, `viewModel { }` for ViewModels
6. **Result type** — typed errors, mapped to user-friendly messages
7. **No use-case layer** — ViewModels call DataSource interfaces directly
8. **No Room for onboarding** — DataStore Preferences only (key-value)

---

# Foster Onboarding Implementation Specification

This section is the concrete implementation plan for Foster. Use this section when implementing onboarding persistence, ViewModels, permissions, and final sync.

The current Foster onboarding UI exists mostly as local `remember { mutableStateOf(...) }` screen state. The implementation goal is to move onboarding to a predictable MVI structure with a shared local draft store.

## Actual Foster Onboarding Screens

This is the current Foster onboarding flow. This list is the source of truth for implementation.

```text
Welcome
  -> Phone
  -> CodeVerification
  -> Name
  -> Contact
  -> Group
  -> Reminder
  -> TimeReminder
  -> CustomReminder
  -> AddNote
  -> Notification
  -> Onboarding complete / main app
```

Current route declarations live in:

```text
shared/src/commonMain/kotlin/app/usefoster/navigation/Screen.kt
```

Current screen wiring lives in:

```text
onboarding/src/commonMain/kotlin/app/usefoster/onboarding/OnboardingApp.kt
```

Do not add these Voqal screens to Foster:

1. Email.
2. Password.
3. Username.
4. Language.
5. Interests.

Do not create backend columns or draft fields for those Voqal-only screens unless the Foster product scope changes.

## Important Architecture Decision

Foster onboarding must use **local draft persistence + final batch sync**.

Do **not** implement full offline-first database sync for onboarding.

### What This Means

During onboarding:

1. Each screen writes user input into a local `OnboardingDraftStore`.
2. The draft is persisted locally using DataStore Preferences.
3. The app can restore onboarding progress after app restart.
4. Most onboarding screens should not call the backend.
5. The final onboarding step submits the completed draft to the backend in one batch flow.
6. After successful final sync, clear the local draft.

### What This Does Not Mean

Do not build:

1. Room tables for temporary onboarding form values.
2. SQLDelight tables for temporary onboarding form values.
3. Sync queues.
4. Conflict resolution.
5. Per-field server syncing during every screen.
6. A generic offline-first engine.

That is too heavy for onboarding. Onboarding is a short linear setup flow, not a long-lived offline domain model.

## Storage Choice

Use DataStore Preferences for onboarding draft persistence.

Use backend tables only for durable user data after onboarding is submitted.

### Use DataStore For

Store temporary onboarding draft values:

1. Phone number.
2. Verification/session state if needed.
3. User display name.
4. Selected profile photo URI or selected avatar ID.
5. Groups created or selected during onboarding.
6. Reminder day/frequency.
7. Reminder time.
8. Custom reminder drafts.
9. Note drafts.
10. Notification permission result.
11. Current onboarding step.
12. Last updated timestamp.

### Use Backend For

Store completed durable app data:

1. User profile.
2. Groups.
3. Reminder preferences.
4. Custom reminders.
5. Notes.
6. Notification settings.
7. Onboarding completion state.

## Module Layout

Implement onboarding under:

```text
onboarding/src/commonMain/kotlin/app/usefoster/onboarding/
```

Use this structure:

```text
onboarding/
├── data/
│   ├── DataStoreOnboardingDraftDataSource.kt
│   ├── OnboardingDraftJson.kt
│   ├── SupabaseOnboardingProfileDataSource.kt       # Only when backend is wired
│   └── dto/
│       └── OnboardingDtos.kt
├── domain/
│   ├── CustomReminderDraft.kt
│   ├── NoteDraft.kt
│   ├── OnboardingDraft.kt
│   ├── OnboardingDraftLocalDataSource.kt
│   ├── OnboardingProfileDataSource.kt
│   ├── OnboardingProfileError.kt
│   ├── OnboardingProfileErrorMessages.kt
│   └── Result.kt
├── presentation/
│   ├── OnboardingDraftStore.kt
│   └── navigation/
│       └── OnboardingRoutes.kt
├── welcome/
├── permissions/
│   ├── Permission.kt
│   └── PermissionController.kt
├── phone/
│   ├── PhoneAction.kt
│   ├── PhoneEvent.kt
│   ├── PhoneState.kt
│   ├── PhoneViewModel.kt
│   ├── PhoneScreen.kt
│   ├── CodeVerificationAction.kt
│   ├── CodeVerificationEvent.kt
│   ├── CodeVerificationState.kt
│   ├── CodeVerificationViewModel.kt
│   └── CodeVerificationScreen.kt
├── name/
├── contact/
├── group/
├── dayreminder/
├── timereminder/
├── customreminder/
├── addnote/
└── notification/
```

Platform-specific files:

```text
onboarding/src/androidMain/kotlin/app/usefoster/onboarding/
├── data/
│   └── DataStoreFactory.android.kt
└── permissions/
    └── PermissionController.android.kt

onboarding/src/iosMain/kotlin/app/usefoster/onboarding/
├── data/
│   └── DataStoreFactory.ios.kt
└── permissions/
    └── PermissionController.ios.kt
```

## Domain Models

Create these models in `domain/`.

```kotlin
data class OnboardingDraft(
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val name: String = "",
    val profilePhotoUri: String? = null,
    val selectedAvatarId: String? = null,
    val groups: List<GroupDraft> = emptyList(),
    val reminderFrequency: ReminderFrequency? = null,
    val reminderTime: ReminderTimeDraft? = null,
    val customReminders: List<CustomReminderDraft> = emptyList(),
    val notes: List<NoteDraft> = emptyList(),
    val notificationPermissionAsked: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val lastUpdatedAtMillis: Long = 0L,
)
```

```kotlin
enum class OnboardingStep(val index: Int) {
    Welcome(0),
    Phone(1),
    CodeVerification(2),
    Name(3),
    Contact(4),
    Group(5),
    DayReminder(6),
    TimeReminder(7),
    CustomReminder(8),
    AddNote(9),
    Notification(10),
    Complete(11),
}
```

```kotlin
data class GroupDraft(
    val id: String,
    val name: String,
    val color: String? = null,
)
```

```kotlin
enum class ReminderFrequency {
    Daily,
    Weekly,
    Monthly,
    Yearly,
    None,
}
```

```kotlin
data class ReminderTimeDraft(
    val hour: Int,
    val minute: Int,
)
```

```kotlin
data class CustomReminderDraft(
    val id: String,
    val title: String,
    val description: String = "",
    val recurrence: ReminderFrequency = ReminderFrequency.None,
    val dateEpochMillis: Long? = null,
)
```

```kotlin
data class NoteDraft(
    val id: String,
    val title: String,
    val body: String = "",
)
```

Rules:

1. Use stable IDs for drafts that can appear in a list.
2. Do not store formatted dates as the source of truth. Store epoch millis and format in the UI.
3. Do not store UI-only flags in the draft unless they must survive restart.
4. Do not put Compose types, Android types, or iOS types in domain models.
5. Do not add `email`, `password`, `username`, `selectedLanguageId`, or `selectedInterestIds` to Foster's draft model based on the Voqal reference.

## DataStore Data Source

Create an interface:

```kotlin
interface OnboardingDraftLocalDataSource {
    suspend fun getDraft(): OnboardingDraft
    suspend fun saveDraft(draft: OnboardingDraft)
    suspend fun clearDraft()
}
```

Implement it with DataStore Preferences:

```kotlin
class DataStoreOnboardingDraftDataSource(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : OnboardingDraftLocalDataSource {
    private object Keys {
        val DraftJson = stringPreferencesKey("onboarding_draft_json")
    }

    override suspend fun getDraft(): OnboardingDraft {
        return dataStore.data
            .map { prefs ->
                val encoded = prefs[Keys.DraftJson]
                if (encoded == null) {
                    OnboardingDraft()
                } else {
                    json.decodeFromString<OnboardingDraft>(encoded)
                }
            }
            .first()
    }

    override suspend fun saveDraft(draft: OnboardingDraft) {
        dataStore.edit { prefs ->
            prefs[Keys.DraftJson] = json.encodeToString(draft)
        }
    }

    override suspend fun clearDraft() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.DraftJson)
        }
    }
}
```

Recommendation:

Use one JSON preference key for the whole draft, because Foster onboarding contains nested lists (`groups`, `customReminders`, `notes`). This is simpler and safer than spreading complex nested data over many preference keys.

Required:

1. Add `@Serializable` to draft models.
2. Add Kotlin serialization plugin if not already applied.
3. Use `ignoreUnknownKeys = true` in Json so future draft fields do not break restore.

```kotlin
val onboardingJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
```

## OnboardingDraftStore

Create one shared store for the whole onboarding flow.

```kotlin
class OnboardingDraftStore(
    private val localDataSource: OnboardingDraftLocalDataSource,
    private val clock: Clock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    init {
        scope.launch {
            _draft.value = localDataSource.getDraft()
        }
    }

    fun update(transform: (OnboardingDraft) -> OnboardingDraft) {
        val nextDraft = transform(_draft.value)
            .copy(lastUpdatedAtMillis = clock.nowMillis())

        _draft.value = nextDraft

        scope.launch {
            localDataSource.saveDraft(nextDraft)
        }
    }

    fun clear() {
        _draft.value = OnboardingDraft()
        scope.launch {
            localDataSource.clearDraft()
        }
    }
}
```

Rules:

1. Every screen ViewModel reads initial state from `OnboardingDraftStore.draft`.
2. Every meaningful user input writes through to `OnboardingDraftStore`.
3. The draft store owns persistence. Screens and ViewModels must not call DataStore directly.
4. Do not create one DataStore data source per screen.

## Per-Screen MVI Pattern

Each onboarding screen should have these files:

```text
FeatureState.kt
FeatureAction.kt
FeatureEvent.kt
FeatureViewModel.kt
FeatureScreen.kt
```

Example:

```text
name/
├── NameAction.kt
├── NameEvent.kt
├── NameState.kt
├── NameViewModel.kt
└── NameScreen.kt
```

### State

State contains UI state only.

```kotlin
data class NameState(
    val name: String = "",
    val isContinueEnabled: Boolean = false,
    val errorMessage: String? = null,
)
```

### Action

Action represents user intent.

```kotlin
sealed interface NameAction {
    data class NameChanged(val name: String) : NameAction
    data object ContinueClicked : NameAction
    data object BackClicked : NameAction
}
```

### Event

Event represents one-shot side effects.

```kotlin
sealed interface NameEvent {
    data object NavigateNext : NameEvent
    data object NavigateBack : NameEvent
    data class ShowSnackbar(val message: String) : NameEvent
}
```

### ViewModel

The ViewModel processes actions, updates state, writes to draft store, and emits events.

```kotlin
class NameViewModel(
    private val draftStore: OnboardingDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(
        NameState(name = draftStore.draft.value.name)
    )
    val state = _state.asStateFlow()

    private val _events = Channel<NameEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: NameAction) {
        when (action) {
            is NameAction.NameChanged -> {
                val value = action.name
                _state.update {
                    it.copy(
                        name = value,
                        isContinueEnabled = value.trim().isNotEmpty(),
                        errorMessage = null,
                    )
                }
                draftStore.update {
                    it.copy(
                        name = value,
                        currentStep = OnboardingStep.Name,
                    )
                }
            }

            NameAction.ContinueClicked -> {
                if (_state.value.name.trim().isEmpty()) {
                    _state.update { it.copy(errorMessage = "Enter your name") }
                    return
                }
                viewModelScope.launch {
                    _events.send(NameEvent.NavigateNext)
                }
            }

            NameAction.BackClicked -> {
                viewModelScope.launch {
                    _events.send(NameEvent.NavigateBack)
                }
            }
        }
    }
}
```

Rules:

1. No navigation directly from button `onClick`.
2. Button `onClick` sends an Action to the ViewModel.
3. ViewModel emits Event for navigation.
4. Screen collects Event and calls navigation callback.
5. Screen should not own business state with `remember { mutableStateOf(...) }` except tiny transient UI state such as menu expanded, sheet visible, or text field focus.

## Screen Migration Order

Before migrating any screen, first implement the shared foundation:

1. Domain models (OnboardingDraft, OnboardingStep, GroupDraft, etc.)
2. OnboardingDraftLocalDataSource interface
3. DataStoreOnboardingDraftDataSource
4. Platform DataStore factories
5. OnboardingDraftStore
6. DI/provider entry point

Then migrate screens in this order:

1. `phone/PhoneScreen`
2. `phone/CodeVerificationScreen`
3. `name`
4. `contact`
5. `group`
6. `dayreminder`
7. `timereminder`
8. `customreminder`
9. `addnote`
10. `notification`
11. `welcome` — only if it later needs persisted state or ViewModel logic

Do not try to rewrite all screens at once. Migrate one screen, compile, then continue.

Each screen should update `OnboardingDraft.currentStep` to its matching `OnboardingStep` when the user reaches or edits that screen.

## Permission Architecture

Foster already needs notification permission and will need more permissions later. Keep permissions generic.

Use:

```text
permissions/
├── Permission.kt
└── PermissionController.kt
```

Common API:

```kotlin
enum class Permission {
    Notification,
    Camera,
    Photos,
    Contacts,
}

enum class PermissionStatus {
    Granted,
    Denied,
    PermanentlyDenied,
    Limited,
    NotDetermined,
}

interface PermissionController {
    fun requestPermission(
        permission: Permission,
        onResult: (PermissionStatus) -> Unit,
    )
}

@Composable
expect fun rememberPermissionController(): PermissionController
```

Rules:

1. Do not put Android permission APIs in common code.
2. Do not put iOS permission APIs in common code.
3. Common code asks for `Permission.Notification`, `Permission.Camera`, etc.
4. Platform `actual` code maps the common permission to platform APIs.
5. Add platform usage strings when iOS requires them.
6. Add Android manifest permissions when Android requires them.

Notification permission behavior:

1. Android 13 and above: request `android.permission.POST_NOTIFICATIONS`.
2. Android 12 and below: treat notification permission as granted.
3. iOS: call `UNUserNotificationCenter.requestAuthorizationWithOptions`.
4. Store the result in `OnboardingDraft.notificationPermissionAsked` and `notificationPermissionGranted`.
5. Do not block onboarding forever if the user denies notification permission. Let the user continue.

## Final Batch Sync

Final sync should happen at the last onboarding step, probably after notification permission or when the user taps the final continue button.

Create:

```kotlin
interface OnboardingProfileDataSource {
    suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError>
    suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError>
}
```

Backend sync should map the draft to durable backend records:

1. Ensure user profile exists.
2. Save phone number / verified phone status if this is part of the auth backend.
3. Save name and avatar/profile photo.
4. Save groups.
5. Save reminder frequency and reminder time.
6. Save custom reminders.
7. Save notes.
8. Save notification settings.
9. Set `profiles.onboarding_step = Complete.index`.
10. Set `profiles.onboarding_completed_at`.

Only clear local draft after the full final sync succeeds.

If final sync fails:

1. Keep local draft.
2. Show user-friendly error.
3. Allow retry.
4. Do not lose user input.

## Backend Schema Recommendation

This is a suggested server-side shape. Exact SQL depends on the backend.

```text
profiles
├── id
├── phone_number
├── phone_verified
├── display_name
├── avatar_url
├── selected_avatar_id
├── onboarding_step
├── onboarding_completed_at
├── created_at
└── updated_at

groups
├── id
├── owner_user_id
├── name
├── color
├── created_at
└── updated_at

user_reminder_preferences
├── user_id
├── reminder_frequency
├── reminder_hour
├── reminder_minute
├── created_at
└── updated_at

custom_reminders
├── id
├── user_id
├── title
├── description
├── recurrence
├── date_epoch_millis
├── created_at
└── updated_at

notes
├── id
├── user_id
├── title
├── body
├── created_at
└── updated_at

notification_settings
├── user_id
├── permission_asked
├── permission_granted
├── created_at
└── updated_at
```

Rules:

1. Backend tables are for durable app data.
2. Do not mirror the entire local `OnboardingDraft` as one backend JSON blob unless the backend is temporary.
3. Store normalized records for things the app will use after onboarding.
4. Server `onboarding_step` is for routing/resume decisions, not for storing all form data.
5. Do not add Voqal-only columns such as `email`, `password`, `username`, `language`, or `interests` unless Foster later adds those product features.

## Resume Behavior

At app startup:

1. If user is not authenticated, show auth/onboarding start.
2. If user is authenticated, ask backend for `onboarding_step`.
3. If backend says onboarding complete, go to main app.
4. If backend says onboarding incomplete, restore local draft if available.
5. Route to the local draft `currentStep` if it exists.
6. If no local draft exists, route using backend `onboarding_step`.

Important:

Local draft helps recover in-progress input on the same device. Backend step helps route across app restarts and possibly across devices.

## Error Handling

Use typed errors:

```kotlin
sealed interface OnboardingProfileError {
    data object NotAuthenticated : OnboardingProfileError
    data object Network : OnboardingProfileError
    data object Server : OnboardingProfileError
    data object Unknown : OnboardingProfileError
}
```

Map errors to user messages:

```kotlin
fun OnboardingProfileError.toUserMessage(): String = when (this) {
    OnboardingProfileError.NotAuthenticated -> "Sign in again to continue"
    OnboardingProfileError.Network -> "Check your connection and try again"
    OnboardingProfileError.Server -> "We could not save your setup. Try again"
    OnboardingProfileError.Unknown -> "Something went wrong. Please try again"
}
```

Do not expose raw exceptions to UI.

## Implementation Checklist

Use this checklist for the implementation agent:

1. Add serialization support if missing.
2. Add DataStore dependency if missing.
3. Create domain draft models.
4. Create `OnboardingDraftLocalDataSource`.
5. Create `DataStoreOnboardingDraftDataSource`.
6. Create platform DataStore factories for Android and iOS.
7. Create `OnboardingDraftStore`.
8. Add dependency injection or a simple app-level provider for the draft store.
9. Migrate one screen to ViewModel MVI.
10. Verify the screen writes to draft store.
11. Verify app restart restores the draft.
12. Repeat screen migration step-by-step.
13. Keep permission handling in `permissions/`.
14. Wire notification permission through `PermissionController`.
15. Add final batch sync data source.
16. Clear local draft only after successful final sync.
17. Run Android compile.
18. Run iOS simulator compile.
19. Confirm no Voqal-only screens or fields were introduced.

## Things To Avoid

Do not:

1. Put all onboarding state in `remember` and lose it on app restart.
2. Navigate directly from UI buttons when ViewModel validation is needed.
3. Store formatted date strings as database values.
4. Use Room for temporary onboarding draft.
5. Call backend on every simple field change.
6. Clear draft before backend final sync succeeds.
7. Mix platform permission APIs into common code.
8. Add AndroidX dependencies that require a higher AGP or compileSdk than the project uses.
9. Rewrite unrelated UI while adding persistence.
10. Implement a broad offline-first sync system for onboarding.

## Acceptance Criteria

Implementation is correct when:

1. User can move through onboarding normally.
2. User input survives process death/app restart.
3. Current step is restored.
4. Notification permission dialog appears from the notification screen.
5. Denying notification permission still allows onboarding to continue.
6. Final submit saves all completed onboarding data to backend.
7. Local draft is cleared only after final submit succeeds.
8. Android build passes.
9. iOS simulator compile passes.
10. No Room/SQLDelight onboarding draft tables are added.
