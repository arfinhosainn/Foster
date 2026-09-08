# Foster — Compliance TODO (prioritized)

Generated from the Phase 1 audit. "P0" = blocks store submission or creates legal exposure; "P1" = required for the documents to be fully truthful; "P2" = hardening.

## P0 — before store submission

- [x] **1. Revert the testing-only exported receiver — DONE 2026-09-08 (verified in merged manifest).** `androidApp/src/main/AndroidManifest.xml:45-49` sets `CheckInReminderReceiver` to `exported="true"` for manual QA. The code comment itself says "MUST be reverted to exported=\"false\" before shipping." An exported broadcast receiver is a security surface (any app can fire your reminders).
- [ ] **2. Publish the web pages.** `https://fosterapp.framer.website/privacy`, `/terms`, `/delete-account` must be live, public, no-login, no-PDF-only, no geo-blocking before you fill the Play Data safety form and the Apple review notes. HTML drafts: `website/privacy.html`, `website/terms.html` in this repo.
- [x] **3. Verify Supabase backup retention — RESOLVED (2026-09-08).** Supabase Free Plan includes **no scheduled backups** (confirmed via dashboard banner: "Free Plan does not include project backups; Pro adds up to 7 days"). Therefore no backup copies of user data exist. Privacy Policy §8 updated to state this. **Revisit if you ever upgrade to Pro** (7-day backup window would need the retention wording adjusted).
- [x] **4. Add `PrivacyInfo.xcprivacy` to the iOS app — DONE 2026-09-08.** Created `iosApp/iosApp/PrivacyInfo.xcprivacy`; Xcode project uses synchronized folder groups so it is picked up automatically on next build. Still verify RevenueCat's SDK ships its own signed manifest in the linked version, and check whether the Google Identity SDK variant Supabase `compose-auth` links on iOS ships one.
- [ ] **5. Decide the Gemini data question.** In `STORE_DISCLOSURES.md` §1a — either enable no-training/commercial terms on the Gemini key or answer "shared" on the Play Data safety form and keep the §5 disclosure as-is.

## P1 — make the app fully match the documents

- [x] **6. Strip unused biometric permissions — DONE 2026-09-08 (`tools:node="remove"`, verified absent from merged manifest).** Original note: `USE_BIOMETRIC`/`USE_FINGERPRINT` arrive transitively via `androidx.credentials` ← Google Identity; no app code uses biometrics. Add to `androidApp/src/main/AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.USE_BIOMETRIC" tools:node="remove" />
  <uses-permission android:name="android.permission.USE_FINGERPRINT" tools:node="remove" />
  ```
  (requires `xmlns:tools="http://schemas.android.com/tools"` on `<manifest>`). Re-run the manifest merge and confirm via the blame report.
- [x] **7. Restrict cloud backup of local data — DONE 2026-09-08 (`backup_rules.xml` + `data_extraction_rules.xml` exclude datastore/ + shared prefs; Privacy Policy §10 wording still valid).** Original note: `android:allowBackup="true"` (`AndroidManifest.xml:16`) with no `dataExtractionRules`. Add rules excluding DataStore preference files (onboarding draft, auth session is already managed by the SDK) or disclose the backup in the Privacy Policy (currently disclosed as a device-backup possibility).
- [ ] **8. Propagate deletion to RevenueCat — ASSIGNED to developer (2026-09-08).** After the edge function deletes the Supabase user, the RevenueCat `appUserID` (linked in `RevenueCatSubscriptionRepository.kt:64-77`) and purchase history remain at RevenueCat. Plan: call RevenueCat's logOut + subscriber data-deletion flow as part of or after the `delete-account` edge function. Until shipped, the Privacy Policy §8 wording ("purchase records may be retained by our subscription processor as required by tax/consumer law") already covers the gap truthfully.
- [x] **9. Add the web delete-account link in Settings — DONE 2026-09-08 (implemented in `SettingScreen.kt` + strings EN/ES; build verified).** Reference diff: `docs/suggested-diff-delete-account-link.md`. Rationale: Play reviewers look for both paths.

## P2 — hardening / lawyer questions

- [x] **10. GDPR Art. 27 EU/UK representative — N/A (2026-09-08).** EU/UK excluded from distribution, so GDPR/UK GDPR obligations (including the Art. 27 representative) do not apply. If distribution there ever begins, revisit Privacy Policy §13, §17 and trader status.
- [ ] **11. Trader status & consumer law.** Bangladesh governing law vs. EEA/UK consumer rights — have counsel confirm the Terms' governing-law clause and mandatory-consumer-rights savings clause are adequate.
- [x] **12. Permission rationale strings — DONE, then superseded by item 16 (2026-09-08).** A prominent-disclosure dialog was implemented first; after the contact-picker switch (item 16) it became unnecessary and was removed — there is no contacts permission left to disclose. Notification permission flow unchanged.
- [x] **13. Debug IP logging — DONE 2026-09-08.** Production warning added to the `brainstorm/index.ts` file header: keep `DEBUG_IP_HEADERS` unset/"false" in prod (logs raw forwarding headers = personal data). Runtime behavior unchanged; flag remains for one-off deployment probes.
- [x] **14. `delete_type_confirm` / destructive-copy localization — DONE 2026-09-08.** Added `delete_confirm_keyword` string ("DELETE"/"ELIMINAR"); `delete_type_confirm` now uses a `%1$s` placeholder; `DeleteAccountBottomSheet` compares, placeholders, and displays the localized keyword (EN/ES).
- [ ] **15. Exclude the EEA/UK from store availability (NEW, 2026-09-08 — required by the decision above).** Play Console → Policy → App content (trader declaration) + Countries/regions: remove all EEA member states + UK. App Store Connect → Pricing & Availability: uncheck all EEA + UK territories. Privacy Policy §13 and the trader answer in STORE_DISCLOSURES.md §2 both rely on this exclusion actually being configured.
- [x] **16. Contact Picker policy (Google, enforced Oct 28, 2026) — DONE 2026-09-08.** Removed `READ_CONTACTS` from the manifest and all permission-request code; import now uses ONLY the Android system contact picker (`ActivityResultContracts.PickContact`) and iOS `CNContactPickerViewController`. Deleted the dead `rememberContactPermissionLauncher` expect/actuals (common/android/ios); removed `NSContactsUsageDescription` from `Info.plist`; removed the now-unneeded disclosure dialog + strings. Privacy Policy §2, STORE_DISCLOSURES §1/§2/§5/Appendix A updated. Result: zero contact permission prompts, nothing to declare in Play Console.

## Verified non-issues (no action needed)

- No ad SDKs, no analytics, no crash SDK, no `AD_ID`, no ATT requirement, no WebView cookies.
- HTTPS-only (no cleartext, no `network_security_config.xml` needed).
- Privacy Policy + Terms already linked in-app (Settings + sign-in screen) — `SettingScreen.kt:89-90, 290-324`, `WelcomeScreen.kt:65-66, 299-301`.
- In-app account deletion fully wired: sheet → data source → edge function → cascade delete (Phase 1 §5).
- No UGC/social features → Apple UGC moderation requirements not triggered.

---

**These documents are drafts prepared from a code audit. They are not legal advice. Have a lawyer review the Privacy Policy and Terms before publication, especially the GDPR/UK representative question, trader status, consumer-law savings clauses, and the Supabase backup-retention wording.**
