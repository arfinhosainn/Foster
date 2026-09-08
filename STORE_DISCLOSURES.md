# Foster — Store Disclosures & Console Answers

Derived from the Phase 1 audit (see **Appendix A** for the code citations backing every claim). Facts current as of commit `61a1183` / HEAD, audited 2026-09-08.

**App identity:** Foster — Grow Your Connection · `app.usefoster` · version 1.0.0 (3) · Android + iOS (KMP) · backend Supabase (us-west-2, per developer) · no ad SDKs, no analytics/crash SDKs, no tracking.

---

## 1. Google Play — Data safety form

Answer "Does your app collect or share any of the required data types?" → **Yes**, then enter:

| Data type | Collected? | Shared? | Required/Optional | Purpose | Encrypted in transit | Deletion supported |
|---|---|---|---|---|---|---|
| **Personal info → Email address** | Yes | No (processor only) | Required | App functionality | Yes | Yes (in-app + web) |
| **Personal info → Name** (user's display name) | Yes | No | Optional | App functionality, Personalization | Yes | Yes |
| **Personal info → User IDs** (Supabase account ID; linked RevenueCat app-user ID) | Yes | No (RevenueCat = processor) | Required | App functionality, Account management | Yes | Yes |
| **Photos → Profile photos** (avatar uploads) | Yes | No | Optional | App functionality, Personalization | Yes | Yes |
| **App activity → Other user-generated content** (contact entries, check-ins + check-in notes, private notes, custom reminders, groups) | Yes | **See §1a (Gemini)** | Optional | App functionality, Personalization | Yes | Yes |
| **Financial info → Purchase history** (via Play Billing → RevenueCat) | Yes | No (processor) | Optional (needed only to buy) | App functionality | Yes | Purchase data: ask RevenueCat; account link deleted with account |
| **Device or other IDs** (RevenueCat device/app-user identifiers; IP address for abuse limiting) | Yes | No | Required | App functionality, Security/fraud prevention | Yes | IP: not retained with account; IDs: yes via account deletion |

Notes for the form: "Shared" excludes service providers processing on your behalf — Google (Supabase/Gemini/Billing), Supabase, and RevenueCat are processors, so answer **No** to sharing, with the Gemini caveat in §1a. "Temporary" processing: the brainstorm edge function processes the request in memory and rate-limits by IP; only the generated suggestions are persisted (as your own data).

### 1a. Google Gemini caveat — resolve before submitting

The brainstorm edge function sends your notes/relationship info to Google's Gemini API (`supabase/functions/brainstorm/llm.ts:16`). Whether this counts as "sharing" in Play's eyes depends on Google's use of the data under your tier of the Gemini API terms (unpaid services may use data to improve products). **Action:** enable the no-training/commercial-terms option on the API key if available, then answer "No (processor)". If you keep default unpaid terms, answer "Yes — shared: App activity → Other user-generated content; purpose: App functionality" and disclose Gemini as a third party in §5 of the Privacy Policy (already listed there).

## 2. Google Play — declarations needed

| Declaration | Answer |
|---|---|
| Account deletion URL | `https://fosterapp.framer.website/delete-account` — **live page required before submission**; in-app deletion exists (verified) |
| Advertising ID permission (`AD_ID`) | **Not declared** — nothing to declare; do not add |
| `USE_EXACT_ALARM` | Declare as **core, user-visible functionality: reminders/alarms** (manifest:7-12 comment documents this; code: `ReminderScheduler.android.kt:78-79`) |
| Contact permissions (Google Contact Picker policy, enforced Oct 28, 2026) | **Compliant — nothing to declare.** No `READ_CONTACTS`; import uses only the system contact picker (Android `ACTION_PICK` via `PickContact`, iOS `CNContactPickerViewController`). If Play Console's contact-permissions form appears, answer that the app uses the system picker and holds no contact permission |
| All-files access / photo-video broad access / `QUERY_ALL_PACKAGES` / SMS / Call Log / Accessibility / Health Connect | **None declared** — no declarations needed |
| Foreground service types | **None** — app has no foreground services |
| Target audience / Families | **Not targeted at children**; do NOT opt into Families program. Target age: 18+ (subscriptions; adult productivity). No child-directed content |
| EEA trader status | **Non-distribution in EEA/UK (decided 2026-09-08):** in Play Console's trader declaration, answer accordingly and **exclude all EEA + UK countries/regions** under Countries/regions availability (see COMPLIANCE_TODO item 15). If you ever distribute a paid-subscription app in the EEA, you would almost certainly be a trader — revisit |
| Data safety form contacts permission | **No `READ_CONTACTS` held** — import uses the Android system contact picker / iOS `CNContactPickerViewController`; only the single user-imported contact's name/photo becomes app data. Answer "Contacts" = **not collected** as a data type; imported data is covered under "Other user-generated content" |

## 3. Apple App Privacy nutrition label

| Apple category | Collected? | Data | Purpose | Linked to identity | Tracking |
|---|---|---|---|---|---|
| **Contact Info** | Yes | Email address; name | App functionality | Yes | No |
| **Identifiers** | Yes | User ID (Supabase); app-user ID / device ID (RevenueCat) | App functionality, Analytics (subscription analytics) | Yes | No |
| **Purchases** | Yes | Purchase history / entitlements | App functionality | Yes | No |
| **User Content** | Yes | Contact entries, check-ins + notes, private notes, custom reminders, groups, avatar photo | App functionality | Yes | No |
| **Other Data** | Yes | IP address (in-memory, per-request rate limiting) | Security / fraud prevention | No | No |
| Health & Fitness / Financial (non-purchase) / Location / Sensitive Info / Contacts (address book) / Browsing / Search History / Diagnostics / Surroundings / Body | **No** | — | — | — | — |

`NSUserTrackingUsageDescription`: **not needed** — the app performs no tracking (no ad SDKs, no AD_ID/IDFA use, no cross-app linking).

## 4. Apple privacy manifest — `PrivacyInfo.xcprivacy`

**Created 2026-09-08:** `iosApp/iosApp/PrivacyInfo.xcprivacy` (the Xcode project uses synchronized folder groups, so it is added to the target automatically). Content below — keep in sync:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>NSPrivacyTracking</key>
    <false/>
    <key>NSPrivacyTrackingDomains</key>
    <array/>
    <key>NSPrivacyCollectedDataTypes</key>
    <array>
        <!-- Email + name (sign-in/profile) -->
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeEmailAddress</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><true/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string></array>
        </dict>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeName</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><true/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string></array>
        </dict>
        <!-- User content: notes, check-ins, contact entries, photos -->
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeOtherUserContent</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><true/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string></array>
        </dict>
        <!-- User ID + device ID (RevenueCat linking) -->
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeUserID</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><true/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string></array>
        </dict>
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeDeviceID</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><true/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string>
            <string>NSPrivacyCollectedDataTypePurposeAnalytics</string></array>
        </dict>
        <!-- Purchase history -->
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypePurchaseHistory</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><true/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string></array>
        </dict>
        <!-- Other: IP (per-request rate limiting, not linked to identity) -->
        <dict>
            <key>NSPrivacyCollectedDataType</key>
            <string>NSPrivacyCollectedDataTypeOtherUsageData</string>
            <key>NSPrivacyCollectedDataTypeLinked</key><false/>
            <key>NSPrivacyCollectedDataTypeTracking</key><false/>
            <key>NSPrivacyCollectedDataTypePurposes</key>
            <array><string>NSPrivacyCollectedDataTypePurposeAppFunctionality</string></array>
        </dict>
    </array>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <!-- UserDefaults (onboarding draft, swipe hints, paywall state) -->
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array><string>CA92.1</string></array>
        </dict>
        <!-- File timestamp APIs (DataStore / file-backed preferences) -->
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array><string>C617.1</string></array>
        </dict>
    </array>
</dict>
</plist>
```

**Bundled SDKs that must ship their own signed privacy manifest:**

- **RevenueCat iOS SDK** — on Apple's required-SDK list; verify the linked version bundles a signed `PrivacyInfo.xcprivacy` (RevenueCat has shipped signed manifests since v5).
- **Google Identity / Google Sign-In SDK** (if Supabase `compose-auth` links it on iOS for `googleNativeLogin`) — Google ships signed manifests in recent versions.
- **Supabase-kt / Ktor (Darwin)** — Kotlin/Native libraries, not on Apple's required-SDK list; no separate manifest expected. The app-level manifest above covers their required-reason API usage. Mention this in App Review notes if asked.

## 5. Prominent-disclosure moments + suggested copy

| Moment | Where | Copy |
|---|---|---|
| **Contacts** | N/A — changed 2026-09-08 | **No permission prompt and no disclosure needed** — import uses the OS contact picker (Android `ACTION_PICK`, iOS `CNContactPickerViewController`), which requires no permission on either platform. `NSContactsUsageDescription` was removed from `Info.plist` accordingly |
| **Notifications (Android 13+ `POST_NOTIFICATIONS`)** | Onboarding, before reminders are set | "Foster sends you reminders to check in with the people you care about. Allow notifications so reminders can reach you." |
| **Exact alarms (`USE_EXACT_ALARM`)** | No prompt needed (auto-granted for reminder apps) | Play Console declaration: core function = reminders (see §2) |
| **AI brainstorm** | First use of brainstorm (recommended one-time note) | "To suggest conversation topics, Foster sends the notes you wrote about this person to an AI service (Google Gemini). Only you can trigger this, once per person per day." |
| **ATT / `NSUserTrackingUsageDescription`** | N/A | Not required — the app does no tracking |

## Appendix A — code citations backing every claim

| Claim | Citation |
|---|---|
| Permission set incl. `USE_EXACT_ALARM` (no `READ_CONTACTS` — removed 2026-09-08) | `androidApp/src/main/AndroidManifest.xml` |
| Library-injected permissions (`USE_BIOMETRIC`, `ACCESS_NETWORK_STATE`, `BILLING`) | manifest-merger blame report (release) lines 21-24, `androidApp/build/intermediates/manifest_merge_blame_file/` |
| System picker returns one contact; only name+photo read | `shared/src/androidMain/.../ContactPicker.android.kt` (`PickContact` + `readContact`) |
| Exact alarms used | `shared/src/androidMain/.../ReminderScheduler.android.kt:78-79` |
| Google/Apple sign-in | `onboarding/src/commonMain/.../SupabaseConfig.kt:46-49` |
| Email/name/avatar profile fields | `onboarding/src/commonMain/.../SupabaseOnboardingProfileDataSource.kt:34-40, 221-260` |
| Avatar upload to Supabase Storage | `SupabaseOnboardingProfileDataSource.kt:189-193` |
| Contact/check-in/note/reminder data transmitted | `home/src/commonMain/.../SupabaseContactDataSource.kt:30-108` |
| Brainstorm → Gemini | `supabase/functions/brainstorm/llm.ts:14-35` |
| IP rate limiting (30/day/IP) | `supabase/functions/brainstorm/index.ts` (`PER_IP_DAILY_LIMIT`, `extractClientIp`) |
| RevenueCat user-ID linking | `shared/src/commonMain/.../RevenueCatSubscriptionRepository.kt:64-77` |
| Free-trial handling | `RevenueCatSubscriptionRepository.kt:197-234` |
| In-app deletion UI | `home/src/commonMain/.../DeleteAccountBottomSheet.kt:60-68, 146-175` |
| Deletion → edge function | `home/src/commonMain/.../SupabaseDeleteAccountDataSource.kt:40-65` |
| Hard delete + cascade + avatar cleanup | `supabase/functions/delete-account/index.ts:10-13, 60-83` |
| Policy/Terms linked in Settings + sign-in | `SettingScreen.kt:89-90, 290-324`; `WelcomeScreen.kt:65-66, 299-301` |
| Local-only storage (DataStore/NSUserDefaults) | `onboarding/.../DataStoreOnboardingDraftDataSource.kt`, `.../NSUserDefaultsOnboardingDraftDataSource.kt` |
| `allowBackup="true"` | `AndroidManifest.xml:16` |
| No analytics/ad/crash SDKs | grep: zero matches for `logEvent|Firebase|Analytics|Crashlytics|Sentry|AD_ID` in app source; `gradle/libs.versions.toml` dependency list |
| `PrivacyInfo.xcprivacy` missing at audit time | `find` → zero results (audit date 2026-09-08); **added 2026-09-08** → `iosApp/iosApp/PrivacyInfo.xcprivacy` |
| Subscriptions (monthly/annual, "unlimited") | `RevenueCatSubscriptionRepository.kt:163-195` |



