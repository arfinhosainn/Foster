# Foster — ToS & Privacy Policy Data Inventory (code-verified)

> Pulled directly from this repo (branch `main`, commit `38b957d`) on 2026-09-05.
> Every claim below has a file reference so you can verify it yourself.
>
> **⚠️ Naming mismatch:** the *folder* is `Nekko`, but the *app* in this repo is
> **Foster** (`app.usefoster`) — repo `github.com/arfinhosainn/Foster`.
> Only write policies for this app from this document. If "Nekko", "ROOTS", or
> a live-audio app are separate products, their data practices are **not** in
> this repo and must be documented separately.

---

## 1. App identity

| Field | Value | Source |
|---|---|---|
| Public app name | **Foster** | `androidApp/src/main/res/values/strings.xml` (`app_name`) |
| Android package ID | `app.usefoster` | `androidApp/build.gradle.kts` (`applicationId`) |
| iOS bundle ID | `app.usefoster.Foster` (config has `$(TEAM_ID)` suffix) | `iosApp/Configuration/Config.xcconfig` |
| Android version | 1.0.0 (versionCode 1) | `androidApp/build.gradle.kts` |
| iOS version | 1.0 (CFBundleShortVersionString) / build 1 | `iosApp/Configuration/Config.xcconfig` |
| Deep-link scheme | `app.usefoster://auth-callback` | `AndroidManifest.xml`, `iosApp/Info.plist` |
| Repo | `https://github.com/arfinhosainn/Foster.git` | `git remote -v` |

## 2. Platforms & minimums

- **Android + iOS** on one Kotlin Multiplatform (Compose Multiplatform) codebase. — `README.md`
- Android: `minSdk 24` (Android 7.0), `targetSdk 35`, `compileSdk 36`. — `gradle/libs.versions.toml`
- iOS: `IPHONEOS_DEPLOYMENT_TARGET = 18.2`. — `iosApp/iosApp.xcodeproj/project.pbxproj`
- Languages: **English** (default) and **Spanish** (`values-es`), RTL supported. — `*/composeResources/values*/strings.xml`

## 3. Legal entity / controller — **NEEDS YOUR INPUT**

Not present anywhere in the code. You must supply:
- Company or individual name + registered address (data controller)
- Support / privacy contact email (used for data-subject requests)
- Privacy Policy & ToS hosted URLs

The app UI already has **Terms & Conditions** and **Privacy Policy** rows, now **wired to the hosted docs**:
- Welcome screen: "By continuing, you accept our Terms & Conditions and Privacy Policy." — links open `https://fosterapp.framer.website/terms` and `https://fosterapp.framer.website/privacy` via `LocalUriHandler` — `onboarding/.../welcome/WelcomeScreen.kt`
- Settings screen: "Terms of Service" and "Privacy" rows open the same URLs. — `home/.../presentation/settings/SettingScreen.kt`

Hosted URLs:
- Terms: `https://fosterapp.framer.website/terms`
- Privacy: `https://fosterapp.framer.website/privacy`

## 4. Sign-up & authentication — Google + Apple OAuth only

- Sign-in methods (both on welcome screen): **Continue with Google**, **Continue with Apple**. — `onboarding/.../welcome/WelcomeScreen.kt`
- Backed by **Supabase Auth** via `createAppSupabaseClient()` with `googleNativeLogin(serverClientId=...)` and `appleNativeLogin()`. — `onboarding/.../data/supabase/SupabaseConfig.kt`
- Google client IDs:
  - Android web client: `874656360216-clvksukjpp8jpmusoo93mv11auiumsdq.apps.googleusercontent.com` — `SupabaseConfig.kt`
  - iOS: `874656360216-h79g2ccnq5t5sihsd8e4u7tpu22rg7k5.apps.googleusercontent.com` + `com.googleusercontent.apps.874656360216-...` URL scheme — `iosApp/iosApp/Info.plist`
- ⚠️ **No phone OTP / phone collection at signup.** (An old SQL migration `onboarding/sql/migration_email.sql` is titled "phone → email", i.e. phone was intentionally dropped — do not claim phone sign-in in the policy.)
- Auth session tokens are persisted on-device and auto-refreshed (`autoLoadFromStorage`, `autoSaveToStorage`, `alwaysAutoRefresh`). — `SupabaseConfig.kt`
- Data received from the OAuth provider: your **email**, **verified-email flag**, your Google/Apple identity. The **email is stored** in the `profiles.email` column. — `onboarding/sql/migration_email.sql`, `SupabaseOnboardingProfileDataSource.kt`
## 5. Data collected during onboarding

Onboarding steps: Name → Contact → Group → Day reminder → Time reminder → Custom reminder → Add note → Notification permission. — `onboarding/.../OnboardingApp.kt`

Collected & stored server-side via the `complete_onboarding` RPC (`onboarding/.../data/supabase/SupabaseOnboardingProfileDataSource.kt` → payload in `OnboardingDtos.kt`, SQL in `onboarding/sql/complete_onboarding.sql` + `migration_v2_erd.sql`):

| Data | Stored as |
|---|---|
| Your display name | `profiles.display_name` / `profiles.full_name` |
| Email (from Google/Apple) + verified flag | `profiles.email`, `profiles.email_verified` |
| Profile photo (if added) — uploaded as a file | Supabase Storage bucket `avatars`, path `avatars/{userId}/profile.{ext}`; public URL in `profiles.avatar_url`. Bucket is public (`publicUrl`). — `SupabaseOnboardingProfileDataSource.kt` |
| Selected avatar (emoji/color) | `profiles.selected_avatar_id` |
| First contact's name | `profiles.contact_name` |
| Contact photo imported from device contacts | **Shown locally only — NOT uploaded.** Only a color is stored (`contacts.avatar_color`). — `ContactPicker.android.kt` / `.ios.kt`, `SupabaseContactDataSource.kt` |
| Groups: "Family" + "Friends" auto-created, plus custom groups (name, color) | `groups` table |
| Reminder preferences (frequency, hour, minute) | `profiles.default_frequency`, `profiles.default_reminder_time` |
| Custom reminders (title, description, recurrence, date) | `custom_reminders` table |
| Notes about the first contact (free text) | `notes` table |
| Whether notifications were asked/granted | `notification_settings` (or profiles fields) |

## 6. Data collected during app use

- **Contacts** added (manual or imported from device contacts): `name`, `avatar_color`, `check_in_frequency`, `reminder_time`, `next/last_check_in_date`, `streak_count`. — `home/.../domain/Contact.kt`, `SupabaseContactDataSource.kt`. **Phone numbers are no longer collected** (the `contacts.phone_number` column was dropped — see `onboarding/sql/migration_drop_contact_phone.sql`; contact import reads name + photo only).
- **Check-ins**: timestamp + optional free-text note (`check_ins` table). — `home/.../domain/CheckIn.kt`
- **Missed check-ins** auto-derived (`missed_check_ins` table). — `onboarding/sql/migration_missed_check_ins.sql`
- **Notes about contacts**: title + body, free text (`notes`, current column name `title/body`). — `home/.../domain/Note.kt`
- **Custom reminders + groups** (create/edit/delete). — `SupabaseContactDataSource.kt`
- **Streaks & badges** — check-in streaks and unlocked flower badges w/ unlock timestamps (`badges`, `user_badges`). — `migration_v2_erd.sql`, `badges.sql`
- **Brainstorm (AI) sessions + generated topics** — stored in `brainstorm_sessions` / `brainstorm_topics`. — `supabase/functions/brainstorm/index.ts`
- **Subscription status** — plan/status/trial end synced to `subscriptions` table + `profiles.subscription_tier`. — `migration_v2_erd.sql` (`sync_subscription_tier`)
- **On-device only (not uploaded):** theme preference (light/dark/system), onboarding draft, notification plan snapshot, paywall gate state — DataStore (Android) / NSUserDefaults (iOS). — `shared/src/*Main/.../theme`, `notifications`, `paywall`, `onboarding/.../OnboardingDraftStore.kt`

### Device contact handling (important for the policy)
- Android: uses the **system contact picker** (`PickContact`), requesting `READ_CONTACTS` only at import time; reads the **single selected** contact's display name and photo (local only). **No phone number is read.** — `shared/src/androidMain/.../contacts/ContactPicker.android.kt`
- iOS: uses `CNContactPickerViewController`; reads selected contact's given/family/organization name and image (local only). **No phone number is read.** — `shared/src/iosMain/.../contacts/ContactPicker.ios.kt`
- **The app does not upload your whole contact list.** Only the one contact you choose is saved, and only name + color are stored server-side.
- `Info.plist` usage string: *"Foster uses your contacts to import a person you want to keep in touch with."*
- **Brainstorm share**: tapping the share control on a brainstorm card opens the **OS share sheet** (`ACTION_SEND` on Android / `UIActivityViewController` on iOS) with the idea pre-filled — nothing auto-sends, no SMS-only path, and **no phone number is used or required**. A long-press offers "Copy to clipboard". — `shared/.../messaging/ShareComposer.*`, `home/.../brainstorm/BrainstormScreen.kt`

## 7. Device permissions requested

**Android** (`androidApp/src/main/AndroidManifest.xml`):
- `READ_CONTACTS` — import a single contact
- `POST_NOTIFICATIONS` — check-in reminders
- `USE_EXACT_ALARM` — core reminder functionality (granted by default on Android 13+, per included comment). `SCHEDULE_EXACT_ALARM` was removed from the manifest (redundant on 13+; no runtime request flow existed for it on 12).
- `RECEIVE_BOOT_COMPLETED` — re-arm reminders after reboot

**iOS** (`iosApp/iosApp/Info.plist`):
- `NSContactsUsageDescription` only. Notifications use `UNUserNotificationCenter` (no plist key needed).

**Not requested:** camera, microphone, location, SMS send permission. (The SMS composer only opens the system Messages app pre-filled — the app itself never sends SMS.) — `shared/src/commonMain/.../messaging/SmsComposer.kt`
## 8. Backend & data storage — Supabase

- **Supabase project**: `https://ulrzuzrwilemkcahsvih.supabase.co` — `onboarding/.../data/supabase/SupabaseConfig.kt`
- Hosted **Postgres** + **Auth** + **Storage** (avatar bucket) + **Edge Functions**. Region = the Supabase project's chosen region (not visible in code — confirm in Supabase dashboard).
- Tables (from SQL migrations + code):
  1. `profiles` — id, email, email_verified, display_name, full_name, contact_name, avatar_url, selected_avatar_id, default_frequency, default_reminder_time, subscription_tier, onboarding_step, onboarding_completed_at, created_at, updated_at
  2. `groups` — owner_id, name, color
  3. `custom_reminders` — owner_id, contact_id, title, description, recurrence, date_epoch_millis, time_of_day
  4. `notes` — owner_id, contact_id, title, body
  5. `notification_settings` — permission-ask state
  6. `contacts` — owner_id, name, avatar_color, check_in_frequency, reminder_time, next/last_check_in_date, streak_count, phone_number
  7. `contact_groups` — contact↔group mapping
  8. `check_ins` — contact_id, owner_id, checked_in_at, note
  9. `missed_check_ins` — contact_id, scheduled_date
  10. `brainstorm_sessions` — contact_id, created_at
  11. `brainstorm_topics` — session_id, icon, title, description
  12. `badges` — static catalog
  13. `user_badges` — owner_id, badge_id, unlocked_at
  14. `subscriptions` — owner_id, plan, status, trial_ends_at (synced from RevenueCat)
- **Security**: row-level security enabled; every query is owner-scoped by `auth.uid()`. Service-role key lives only server-side in edge functions. — `migration_v2_erd.sql`, `SupabaseContactDataSource.kt` etc.

## 9. Third-party services

| Provider | What for | Data they receive |
|---|---|---|
| **Supabase** (incl. Supabase, Inc.) | Auth, Postgres storage, storage bucket, edge functions | All app data described above (as data processor) |
| **Google LLC** | Sign in with Google — identity; **Google Gemini API (`gemini-2.5-flash`)** — powers the AI Brainstorm feature | Name + email on sign-up; and when you generate Brainstorm ideas: the selected **contact's name, check-in cadence/streak/dates, your notes, recent check-in note text, and custom reminder titles** are sent to the LLM to produce conversation suggestions. — `supabase/functions/brainstorm/index.ts` + `llm.ts` |
| **Apple Inc.** | Sign in with Apple; App Store billing | Standard Apple-provided account/billing data |
| **RevenueCat** (`purchases-kmp`, v3.6.0) | Subscription/entitlement management, paywall offerings, purchase + restore orchestration | Purchase/entitlement status; **no card/payment details** (Apple/Google Play process payments) — `shared/.../subscription/*` |
| **Google Play** | Android distribution + billing | Standard store billing/account data |
| **AlarmManager / UNUserNotificationCenter** | Local notifications — **no push notification provider (no FCM/APNs)** | Nothing leaves the device — `shared/.../notifications/ReminderScheduler.kt` |

**No ad networks, no advertising SDKs, no cross-device tracking SDKs, no data brokers.**

## 10. Analytics & crash reporting — **NONE present**

A repository-wide search for Firebase / Crashlytics / Sentry / Mixpanel / Amplitude / AppsFlyer / OneSignal / Facebook found nothing in app code. → The Privacy Policy can (currently) truthfully state: *no analytics, no crash reporting, no targeted advertising, no tracking.*

## 11. Subscriptions & payments

- Free tier: **10 contacts** max; **3 Brainstorm generations per month** (per-user, UTC). — `shared/.../subscription/SubscriptionGates.kt`
- One entitlement, `"unlimited"`, unlocks both gates. — `shared/.../subscription/SubscriptionRepository.kt`
- Paywall offers **monthly + annual** packages with optional **free trial** (7-day trial CTA is auto-detected from the store intro offer; price strings come from the store). — `SubscriptionRepository.kt`, `home/.../paywall/PaywallScreen.kt`
- Purchases/restores happen through **Apple App Store / Google Play billing** via RevenueCat; auto-renewing subscriptions — cancellation handled in the OS store settings (not in-app). — `SubscriptionRepository.kt`
- Subscription state is synced server-side (RevenueCat → Supabase `subscriptions` + `profiles.subscription_tier`). — `migration_v2_erd.sql`
- **ToS note:** Apple/Google require you to publish subscription terms (auto-renewal, free-trial terms, how to cancel) as a URL reachable from the app. This is not present in code yet.
## 12. Account deletion & data retention

- **In-app**: Settings → Delete Account → type `DELETE` to confirm → server-side `delete-account` Edge Function → deletes the `auth.users` row. Every public table (profiles, groups, custom_reminders, notes, notification_settings, contacts, user_badges, subscriptions) cascades, and contact_groups/check_ins/brainstorm_sessions/brainstorm_topics/missed_check_ins cascade via contacts; the `avatars/{userId}/` folder is removed too. — `home/.../settings/SettingScreen.kt`, `DeleteAccountBottomSheet.kt`, `supabase/functions/delete-account/index.ts`
- Copy shown to users: *"This action is permanent and cannot be undone. Your account and all of its data will be deleted from our servers…"* — `home/src/commonMain/composeResources/values/strings.xml`
- No retention schedule/SLA is implemented in code (rows persist until account deletion). → **Decide & state a retention policy** (e.g., what happens to data after X days of inactivity; backups).
- ⚠️ **Not covered by account deletion** (say this clearly in your policy):
  1. The **App Store / Play Store subscription is not auto-cancelled** — users must cancel in the store.
  2. **On-device data** (theme pref, onboarding drafts, notification plan snapshot, paywall-gate state) is not purged by the delete flow — clearing app data removes it.

## 13. Children

- **No age-gating or COPPA/GDPR-K handling exists in the code.** Free-text notes + AI processing of notes make age-appropriate design important. You must add a minimum-age statement (13+/16+/18+ per your target markets) to the ToS and, ideally, an actual sign-up gate.

## 14. ToS-relevant facts (building blocks)

- "By continuing, you accept our Terms & Conditions and Privacy Policy" is shown **before** sign-in on the Welcome screen.
- Service description: relationship-maintenance app — add people you care about (from contacts or manually), set check-in cadences/reminders (local notifications), keep private notes, track streaks/badges, and get AI conversation suggestions ("Brainstorm").
- Account required (Google or Apple OAuth).
- Free tier + paid subscription (auto-renewing monthly/annual, trial offers).
- AI feature disclosure: while using Brainstorm, relevant contact notes are processed by Google Gemini to generate suggestions; one use per contact per day; output persisted in your account.
- Acceptable-use basics (personally-inappropriate content of others, privacy of contact data) left to standard boilerplate, but note the app stores **others' names + phone numbers** you import — you're responsible for lawfully importing them.

## 15. Facts that still need YOUR input (not determinable from code)

1. Legal entity name + address (controller) — e.g., *"JS Consultant"* or your own name
2. Support / privacy contact email
3. Target countries (drives GDPR / CCPA / Malaysia PDPA language)
4. Depends on you hosting the docs at the URLs the app already points to — Terms: `https://fosterapp.framer.website/terms`, Privacy: `https://fosterapp.framer.website/privacy` (links are wired in-app)
5. Data-retention policy wording (how long data is kept after account deletion/backups)
6. Minimum age to use the app (and whether to add an age gate)
7. App Store Connect / Play Console support URL + Subscription Terms URL (store-required)
8. Whether English-only docs are enough or Spanish versions are also needed (app ships in ES)