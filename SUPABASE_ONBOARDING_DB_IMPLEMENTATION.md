# Supabase Onboarding DB Implementation Plan

This document describes the online database implementation for Nekko onboarding using Supabase.

The current app already has local draft persistence:

- Android: DataStore Preferences.
- iOS: NSUserDefaults.
- Shared model: `OnboardingDraft`.
- Shared store: `OnboardingDraftStore`.

Supabase should be used only for durable onboarding results after the user finishes onboarding. Do not write every field to Supabase on every screen.

## Architecture Decision

Use:

```text
local draft persistence during onboarding
final batch submit to Supabase when onboarding completes
clear local draft only after successful Supabase submit
```

Do not implement:

1. Per-field Supabase syncing while typing.
2. A sync queue for onboarding.
3. Room/SQLDelight tables for onboarding drafts.
4. Full offline-first conflict resolution for onboarding.

Onboarding is a short setup flow. Local draft persistence is enough while the user moves through screens.

## ViewModel Structure

Yes, each onboarding screen should have its own ViewModel.

Use this pattern:

```text
phone/
├── PhoneAction.kt
├── PhoneEvent.kt
├── PhoneState.kt
├── PhoneViewModel.kt
└── PhoneScreen.kt

name/
├── NameAction.kt
├── NameEvent.kt
├── NameState.kt
├── NameViewModel.kt
└── NameScreen.kt
```

Each screen ViewModel should:

1. Read initial values from `OnboardingDraftStore.draft`.
2. Own UI state for that screen.
3. Process user actions.
4. Write meaningful onboarding data back to `OnboardingDraftStore`.
5. Emit one-shot events for navigation, snackbar messages, permission requests, or final submit result.

Screens should not directly mutate long-lived onboarding form state with:

```kotlin
remember { mutableStateOf(...) }
```

That is only acceptable for temporary UI-only state, such as:

1. Bottom sheet open/closed.
2. Dropdown expanded.
3. Date picker visible.
4. Text field focus state.

## Existing Local Draft Fields

Current local draft data should map to Supabase at final submit:

```kotlin
OnboardingDraft(
    phoneNumber,
    phoneVerified,
    name,
    contactName,
    profilePhotoUri,
    selectedAvatarId,
    groups,
    reminderFrequency,
    reminderTime,
    customReminders,
    notes,
    notificationPermissionAsked,
    notificationPermissionGranted,
    currentStep,
    lastUpdatedAtMillis,
)
```

Do not add Voqal-only fields such as email, password, username, language, or interests unless Nekko adds those product features.

## Supabase Tables

Recommended normalized schema:

```sql
create table profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  phone_number text,
  phone_verified boolean not null default false,
  display_name text,
  contact_name text,
  avatar_url text,
  selected_avatar_id text,
  onboarding_step integer not null default 0,
  onboarding_completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table groups (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  color text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table user_reminder_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  reminder_frequency text,
  reminder_hour integer,
  reminder_minute integer,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table custom_reminders (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  description text not null default '',
  recurrence text not null default 'none',
  date_epoch_millis bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table notes (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table notification_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  permission_asked boolean not null default false,
  permission_granted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
```

## Row Level Security

Enable RLS on every table:

```sql
alter table profiles enable row level security;
alter table groups enable row level security;
alter table user_reminder_preferences enable row level security;
alter table custom_reminders enable row level security;
alter table notes enable row level security;
alter table notification_settings enable row level security;
```

Basic owner policies:

```sql
create policy "Users can read own profile"
on profiles for select
using (auth.uid() = id);

create policy "Users can upsert own profile"
on profiles for insert
with check (auth.uid() = id);

create policy "Users can update own profile"
on profiles for update
using (auth.uid() = id)
with check (auth.uid() = id);
```

For owner-user tables:

```sql
create policy "Users can read own groups"
on groups for select
using (auth.uid() = owner_user_id);

create policy "Users can insert own groups"
on groups for insert
with check (auth.uid() = owner_user_id);

create policy "Users can update own groups"
on groups for update
using (auth.uid() = owner_user_id)
with check (auth.uid() = owner_user_id);
```

Repeat the owner policy pattern for:

1. `custom_reminders.owner_user_id`.
2. `notes.owner_user_id`.

For one-row-per-user tables:

1. `user_reminder_preferences.user_id`.
2. `notification_settings.user_id`.

## Kotlin Data Source

The common domain interface already exists:

```kotlin
interface OnboardingProfileDataSource {
    suspend fun submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError>
    suspend fun getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError>
}
```

Create a Supabase implementation:

```text
onboarding/src/commonMain/kotlin/app/usenekko/onboarding/data/
└── SupabaseOnboardingProfileDataSource.kt
```

Responsibilities:

1. Get current authenticated user ID.
2. Upsert `profiles`.
3. Insert or replace onboarding-created `groups`.
4. Upsert `user_reminder_preferences`.
5. Insert or replace `custom_reminders`.
6. Insert or replace `notes`.
7. Upsert `notification_settings`.
8. Set `profiles.onboarding_step = OnboardingStep.Complete.index`.
9. Set `profiles.onboarding_completed_at = now`.

Only return success when every required write succeeds.

## Final Submit Flow

The final submit should happen from the last onboarding ViewModel, most likely `NotificationViewModel`.

Recommended flow:

```kotlin
class NotificationViewModel(
    private val draftStore: OnboardingDraftStore,
    private val profileDataSource: OnboardingProfileDataSource,
) : ViewModel() {
    fun onAction(action: NotificationAction) {
        when (action) {
            NotificationAction.ContinueClicked -> submitOnboarding()
        }
    }

    private fun submitOnboarding() {
        viewModelScope.launch {
            val draft = draftStore.draft.value
            when (val result = profileDataSource.submitOnboarding(draft)) {
                is Result.Success -> {
                    draftStore.clear()
                    emit(NotificationEvent.NavigateToMainApp)
                }
                is Result.Error -> {
                    emit(NotificationEvent.ShowError(result.error.toUserMessage()))
                }
            }
        }
    }
}
```

Important:

1. Do not clear local draft before Supabase succeeds.
2. If Supabase submit fails, keep local draft.
3. Show a retryable error.
4. Do not navigate to main app until submit succeeds unless the product explicitly allows deferred sync.

## Backend Mapping

Map local draft to Supabase like this:

```text
OnboardingDraft.phoneNumber
  -> profiles.phone_number

OnboardingDraft.phoneVerified
  -> profiles.phone_verified

OnboardingDraft.name
  -> profiles.display_name

OnboardingDraft.contactName
  -> profiles.contact_name

OnboardingDraft.profilePhotoUri
  -> upload to Supabase Storage, then profiles.avatar_url

OnboardingDraft.selectedAvatarId
  -> profiles.selected_avatar_id

OnboardingDraft.groups
  -> groups

OnboardingDraft.reminderFrequency + reminderTime
  -> user_reminder_preferences

OnboardingDraft.customReminders
  -> custom_reminders

OnboardingDraft.notes
  -> notes

OnboardingDraft.notificationPermissionAsked / notificationPermissionGranted
  -> notification_settings

OnboardingDraft.currentStep
  -> profiles.onboarding_step
```

## Photo Upload

If `profilePhotoUri` is a local device URI, it cannot be stored directly as the durable avatar URL.

Recommended flow:

1. Read bytes from local URI on the platform side.
2. Upload to Supabase Storage bucket, for example `avatars`.
3. Use path:

```text
avatars/{user_id}/profile.{ext}
```

4. Store the public URL or signed path in `profiles.avatar_url`.

If the user chooses a built-in avatar, skip upload and store only `selected_avatar_id`.

## Transactions

Supabase client-side calls are not automatically transactional across multiple tables.

Preferred production approach:

1. Create a Postgres RPC function such as `complete_onboarding(payload jsonb)`.
2. Call one RPC from the app.
3. The RPC validates user ID with `auth.uid()`.
4. The RPC writes all tables inside one database transaction.

This avoids partial onboarding writes.

If using client-side multi-table writes first, make the operation idempotent:

1. Use stable IDs where possible.
2. Upsert one-row-per-user tables.
3. Delete and reinsert onboarding-owned list rows if needed.
4. Only mark `profiles.onboarding_step = Complete` after all writes finish.

## Resume Behavior

At app startup:

1. If user is not authenticated, show onboarding/auth start.
2. If user is authenticated, call `getOnboardingStep()`.
3. If Supabase says complete, route to main app.
4. If incomplete, restore local draft.
5. If local draft exists, route using `draft.currentStep`.
6. If no local draft exists, route using Supabase `profiles.onboarding_step`.

Local draft is same-device recovery. Supabase step is account-level routing.

## Implementation Order

1. Finish per-screen ViewModel migration.
2. Add Supabase dependency/config if not already present.
3. Create DTOs for Supabase rows.
4. Implement `SupabaseOnboardingProfileDataSource`.
5. Implement profile photo upload if needed.
6. Create `NotificationViewModel` final submit flow.
7. Clear local draft after successful submit.
8. Add retry UI for final submit failure.
9. Add startup resume routing from local draft/Supabase step.
10. Verify Android and iOS builds.

## Acceptance Criteria

The online DB implementation is complete when:

1. User can finish onboarding while authenticated.
2. Supabase receives profile, group, reminder, custom reminder, note, and notification data.
3. Local draft remains if Supabase submit fails.
4. Local draft clears only after successful submit.
5. Relaunch after successful submit routes to main app.
6. Relaunch before successful submit restores the local onboarding step and form data.
7. RLS prevents one user from reading or writing another user's onboarding data.
8. Android build passes.
9. iOS simulator build passes.
