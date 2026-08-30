# Google Authentication — Current State

> **Audience:** product and engineering stakeholders
>
> **Repository review:** 2026-08-29
>
> This document describes the behavior implemented in the repository. It does not reproduce any keys or secrets. The database status references below reflect the repository’s record that `migration_v2_erd.sql` was applied and verified in Supabase on 2026-08-01.

## Executive summary

- Google sign-in is implemented through Supabase’s native/Compose authentication integration. The app does not implement a separate Google token exchange, email/password flow, or `signUp` call.
- Supabase Auth is the system of record for the authenticated identity. The app uses the Supabase-generated `auth.users.id` UUID as the account key; email is metadata, not the primary user key.
- There is no explicit `isNewUser` flag and no lookup by email. A user is treated as new or returning based on the authenticated UUID and whether that UUID has a `profiles` row with completed onboarding state.
- A completed profile sends the user to Home. An incomplete profile resumes at its saved onboarding step. A confirmed missing profile is provisioned at the Name step, while profile-read and provisioning errors remain retryable instead of sending the user into onboarding.
- Session loading, saving, and refresh are delegated to the Supabase Auth SDK. The app enables automatic session restoration and refresh; it does not manually serialize access or refresh tokens.
- Onboarding drafts are a separate device-local concern. Android uses Preferences DataStore and iOS uses `NSUserDefaults`; the draft contains onboarding fields and progress, not auth tokens.
- The Android OAuth callback boundary is implemented. iOS has Google SDK and URL-scheme configuration, but no Swift-level URL forwarding handler was found; this path should be verified on a real iOS sign-in run.

## 1. Components and responsibilities

| Component | Responsibility |
| --- | --- |
| Google | Authenticates the person with Google and supplies the provider identity through the native auth flow. |
| Supabase Auth | Creates or reuses the Supabase auth account, issues the session, and exposes the authenticated user UUID. |
| `SupabaseClient` | Shared client configured with Auth, PostgREST, Storage, Functions, and native provider login support. |
| `OnboardingApp` | Observes auth session state and decides whether to show Welcome, onboarding, or Home. |
| `SupabaseOnboardingProfileDataSource` | Reads onboarding state, creates a missing profile row, submits onboarding, and reads/updates profile data. |
| Postgres/Supabase | Stores `auth.users`, `profiles`, onboarding data, and user-owned product data with row-level security. |
| Local draft stores | Preserve unfinished onboarding on the current device; they are not the session store. |
| `delete-account` Edge Function | Verifies the caller’s JWT, deletes the Supabase auth user, and lets foreign-key cascades remove account data. |

The main configuration is in `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/SupabaseConfig.kt`, and the shared client is created by `createAppSupabaseClient()`.

## 2. Google sign-in flow

The current flow is:

```text
User taps Continue with Google
        |
        v
WelcomeScreen.rememberSignInWithGoogle()
        |
        v
Supabase ComposeAuth/native provider flow
        |
        v
Supabase Auth creates or reuses auth.users and establishes a session
        |
        v
OnboardingApp receives NativeSignInResult.Success
        |
        v
routeAfterAuth() reads the authenticated user's profiles row
        |
        +--> completed profile  -> Home
        +--> incomplete profile -> saved onboarding step
        +--> read/provisioning error -> retryable error on current surface
```

### What the code does

`WelcomeScreen` creates `rememberSignInWithGoogle` from `supabaseClient.composeAuth`. On success it invokes `onGoogleSignInSuccess`; error, network error, and cancellation are handled in the UI. There is no direct call to `signInWithOAuth`, no Google access-token storage in app code, and no explicit `signUp` branch.

The provider integration therefore owns the first-login versus repeat-login behavior at the Supabase Auth layer:

- On the first successful Google login, Supabase creates the auth account and returns a session.
- On later logins with the same Google identity, Supabase returns the same Supabase auth account/UUID and a new or refreshed session.
- The application itself does not compare Google email addresses to decide whether the account is old or new.

The same Supabase Auth client also registers Apple native login, but Google is the flow documented here.

## 3. How old and new users are identified

### The identity key

The stable account key is the Supabase auth UUID:

```text
Supabase session.user.id
        = auth.users.id
        = profiles.id
        = owner_id/user_id on user-owned rows
```

The `profiles` table is defined with `id` as a primary key referencing `auth.users(id)` with `on delete cascade`. The email received from the authenticated session is copied into the profile during final onboarding submission, but the app does not use email as the account key.

### The actual routing decision

After authentication, `OnboardingApp.routeAfterAuth()` calls `getOnboardingStep()`:

1. `getOnboardingStep()` requires a current session.
2. It selects `onboarding_step` and `onboarding_completed_at` from the authenticated user’s `profiles` row. Row-level security limits access to the current user.
3. A non-null `onboarding_completed_at` takes precedence and maps to `OnboardingStep.Complete`.
4. Otherwise, the numeric `onboarding_step` is mapped to the corresponding onboarding screen.
5. `OnboardingStep.Complete` maps to `Screen.Home`.

This gives the following practical classification:

| Situation | Current result |
| --- | --- |
| Google account has no profile yet | `ensureProfileExists()` creates a profile at the Name step, then the app opens Name. |
| Profile exists but onboarding is incomplete | The app resumes from `profiles.onboarding_step`. |
| Profile has `onboarding_completed_at` | The app goes directly to Home. |
| App was reinstalled but the user signs in with the same Google identity | The same Supabase UUID resolves to the existing server profile and data; completed users bypass onboarding. |
| Profile query fails | The app stays on Splash or Welcome, shows a retryable error, and does not create or overwrite a profile. |

### Important limitation

There is no separate “new user” flag in the application. A zero-row profile query is classified as `ProfileNotFound`; only that result invokes `ensureProfileExists()`, and onboarding starts only after provisioning succeeds. Network, expired/invalid JWT, unexpected response, and other database errors remain on the current surface with a retry action.

This prevents a returning user from being shown onboarding merely because a profile request temporarily fails. A confirmed missing profile is still treated as a new server-side profile, not as a new Google account; the Supabase UUID remains the identity key.

## 4. Session lifecycle and persistence

### Session configuration

`createAppSupabaseClient()` installs Supabase Auth with:

- `autoLoadFromStorage = true` — restore a previously saved session when the client starts.
- `autoSaveToStorage = true` — persist session changes through the Auth SDK.
- `alwaysAutoRefresh = true` — refresh the session as it approaches expiry.
- `enableLifecycleCallbacks = true` — allow lifecycle-aware refresh behavior.
- `host = "auth-callback"` and `scheme = "app.usefoster"` — configure the app callback URL.

The physical token storage mechanism is not implemented or selected by Foster code; it is managed by the Supabase/Ktor/platform library stack. The repository does not expose a custom token database, token serializer, or manual access/refresh-token encryption layer.

### Startup state machine

The app starts at `Screen.Splash` and collects `supabaseClient.auth.sessionStatus` in `OnboardingApp`:

| Auth status | Current behavior while on Splash |
| --- | --- |
| `Initializing` | Wait; no route change. |
| `Authenticated` | Read the current session and call `routeAfterAuth()`. |
| `RefreshFailure` | Try `refreshCurrentSession()` once; an explicit failure shows a retryable network error on Splash. |
| `NotAuthenticated` | Replace the navigation stack with Welcome. |

Once the app has left Splash, auth status events are deliberately ignored by `authSessionAction()`. The app therefore relies on the initial routing decision and explicit navigation for the rest of the flow.

The current session is also checked before profile, contact, group, reminder, note, badge, brainstorm, and account-deletion operations. Home repositories use `session.user.id` as an account key so cached/in-flight data is invalidated when the account changes.

### What is and is not persisted

| Data | Location | Purpose |
| --- | --- | --- |
| Access/refresh session tokens | Supabase Auth SDK/platform-managed storage | Keep the user authenticated across app launches when the SDK can restore the session. |
| Authenticated user UUID/email | Supabase Auth session | Identify the current account for API calls and routing. |
| `profiles` onboarding state | Supabase Postgres | Account-level progress that survives device replacement or reinstall. |
| `OnboardingDraft` | Android Preferences DataStore; iOS `NSUserDefaults` | Same-device recovery of unfinished onboarding. Contains no auth tokens. |
| Contacts and other product data | Supabase Postgres/Storage | Durable user-owned data protected by ownership policies. |

The local draft is saved as JSON under `onboarding_draft_json`. It is updated while onboarding progresses, cleared only after the final RPC succeeds, and retained when the RPC fails so the user can retry. Removing app data or uninstalling the app can remove this draft, but it does not remove the server-side profile or completed account data.

## 5. What is stored after sign-in and onboarding

### Profile provisioning immediately after sign-in

For a newly authenticated UUID with no `profiles` row, `ensureProfileExists()` upserts:

```text
profiles.id = session.user.id
profiles.onboarding_step = Name.index
```

It uses `onConflict("id")` with `ignoreDuplicates = true`, so it does not overwrite an existing profile during this recovery operation.

### Final onboarding submission

The final onboarding action is handled by `NotificationViewModel`. It marks the local draft complete and calls `submitOnboarding()`:

1. The method requires a current Auth session.
2. It derives `email` from `session.user.email` and `emailVerified` from `session.user.emailConfirmedAt`.
3. It encodes the onboarding draft into `CompleteOnboardingPayload`.
4. It calls the Postgres RPC `complete_onboarding(payload)` through PostgREST.
5. On success, the local draft is cleared and the app navigates to the main app.
6. On failure, the draft remains and a retryable error is shown.

The applied schema/migration records that the RPC uses `auth.uid()` rather than trusting a user ID from the client. It writes or seeds:

- `profiles`: email metadata, display/contact name, avatar selection, reminder defaults, onboarding step, and completion timestamp.
- `groups`: built-in Family/Friends groups and onboarding-created custom groups.
- `contacts`: the initial onboarding contact when applicable.
- `contact_groups`: the selected group relationship when applicable.
- `custom_reminders`: onboarding reminders, associated with the initial contact when applicable.
- `notes`: onboarding notes, associated with the initial contact when applicable.
- `notification_settings`: notification permission flags.

The current migration also adds or maintains relationship-tracking tables such as `check_ins`, `brainstorm_sessions`, `brainstorm_topics`, `badges`, `user_badges`, and `subscriptions`. These are account-owned through `auth.users.id` directly or through ownership resolved from contacts.

### Data protection boundary

RLS policies restrict profiles and user-owned records to the authenticated UUID (`auth.uid()`). The `complete_onboarding` RPC is `security definer`, so it explicitly derives the caller from `auth.uid()` and validates ownership-sensitive values inside the transaction.

### Migration caveat

`onboarding/sql/complete_onboarding.sql` and `onboarding/sql/migration_email.sql` contain earlier versions of the schema/RPC, including the old `owner_user_id` naming and the former `user_reminder_preferences` table. The repository’s current-state document says that `onboarding/sql/migration_v2_erd.sql` was applied and replaces that behavior with standardized `owner_id` fields, profile reminder columns, and non-destructive seeding.

Therefore, `complete_onboarding.sql` should not be read in isolation as the live behavior. A fresh environment must apply the migrations in the documented order, or it can behave differently from the recorded live schema.

## 6. Android and iOS callback behavior

### Android

- `MainActivity` lazily creates the shared Supabase client.
- `AndroidManifest.xml` exposes `app.usefoster://auth-callback` as a browsable `VIEW` route.
- The activity uses `singleTop` so a callback can be delivered to an existing activity.
- `onCreate()` calls `supabaseClient.handleDeeplinks(intent)` for cold-start callbacks.
- `onNewIntent()` calls `setIntent(intent)` and then `handleDeeplinks(intent)` for warm callbacks.
- There is no separate Android `GoogleSignIn` implementation; the Google flow is delegated to the shared Supabase Compose Auth integration.

### iOS

- `Info.plist` contains a Google client ID, the Google reverse-client URL scheme, and the `app.usefoster` app scheme.
- The Xcode project links `GoogleSignIn`/`GoogleSignInSwift` and pins `GoogleSignIn-iOS` to 9.2.0.
- The iOS app creates the shared Kotlin Supabase client in `MainViewController` and starts the same `OnboardingApp` flow.
- No Swift `onOpenURL`, application URL callback, scene URL callback, or `GIDSignIn.handle` forwarding code was found in the inspected iOS source.

The iOS configuration is present, but the repository does not show the equivalent native callback-forwarding code that Android has. This should be tested on a real iOS device/simulator before treating iOS Google sign-in as fully verified.

## 7. Logout and account deletion

There is currently no normal standalone logout action in the inspected shared/home code. The implemented account exit path is account deletion:

1. Settings requires typed confirmation.
2. `SupabaseDeleteAccountDataSource` requires a current session and calls the `delete-account` Edge Function.
3. The function requires a Bearer token and calls Supabase Auth `getUser()` to resolve and verify the caller’s UUID.
4. It best-effort removes the user’s avatar files.
5. It deletes the row from `auth.users` using the server-only service role.
6. Foreign keys with `on delete cascade` remove the user’s profile and related account rows.
7. After success, the app best-effort calls `auth.signOut()` and replaces the navigation stack with Welcome.

If deletion fails or the request is unauthenticated, the app shows an error and does not sign out. The service-role key is used only by the Edge Function and is not part of the client configuration.

## 8. Current gaps and points to communicate

1. **No explicit new-user flag:** “New” means “this authenticated UUID has no usable completed profile state,” not “the Google email has never appeared in the app.”
2. **Profile provisioning depends on the live schema and RLS:** only an explicitly empty profile result is provisioned; read or upsert failures remain visible and retryable.
3. **iOS callback forwarding needs verification:** Google SDK linkage and URL schemes exist, but no Swift URL handoff was found.
4. **Token storage is library-managed:** the app enables automatic persistence and refresh but does not document or control the underlying platform storage in its own code.
5. **No normal logout:** the explicit sign-out call is currently part of successful account deletion rather than a user-facing logout feature.
6. **Migration order matters:** the older SQL files contain behavior that differs from the recorded applied `migration_v2_erd.sql` state.
7. **Profile email is copied at onboarding completion:** the Auth session remains the authoritative identity; `profiles.email` is an application profile field and is not the account key.

## 9. Key implementation files

| File | Relevant symbols or purpose |
| --- | --- |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/SupabaseConfig.kt` | Supabase URL/client configuration, Google client registration, session persistence flags. |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/welcome/WelcomeScreen.kt` | `rememberSignInWithGoogle`, provider result handling, sign-in UI. |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/OnboardingApp.kt` | Session observer, `authSessionAction`, `routeAfterAuth`, post-delete sign-out. |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/SupabaseOnboardingProfileDataSource.kt` | Session-gated profile reads, profile provisioning, onboarding RPC call, payload mapping. |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/presentation/LocalOnboardingDraftStore.kt` | Shared client and repository wiring; account key from `session.user.id`. |
| `onboarding/src/androidMain/kotlin/app/usefoster/onboarding/data/DataStoreOnboardingDraftDataSource.kt` | Android draft persistence. |
| `onboarding/src/iosMain/kotlin/app/usefoster/onboarding/data/NSUserDefaultsOnboardingDraftDataSource.kt` | iOS draft persistence. |
| `androidApp/src/main/kotlin/app/usefoster/MainActivity.kt` | Android deep-link handling in `onCreate` and `onNewIntent`. |
| `androidApp/src/main/AndroidManifest.xml` | Android `app.usefoster://auth-callback` route and `singleTop` activity. |
| `iosApp/iosApp/Info.plist` | iOS Google client ID and URL schemes. |
| `iosApp/iosApp/ContentView.swift` | Embeds the Kotlin `MainViewController`; contains no URL callback forwarding. |
| `onboarding/sql/migration_v2_erd.sql` | Current recorded schema/RPC migration and ownership/RLS behavior. |
| `supabase/functions/delete-account/index.ts` | Authenticated account deletion implementation. |
