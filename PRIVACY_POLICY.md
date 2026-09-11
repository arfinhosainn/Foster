# Foster — Privacy Policy

**Effective date: September 8, 2026 · Last updated: September 10, 2026**

- **Who we are (the data controller):** MD. ARFIN HOSSIN PATWARY, trading as **Foster — Grow Your Connection** ("Foster", "we", "us")
- **Registered address:** Taraf Patwary Bari, West Fatehpur, Dagonbhuiyan, Feni - 3900, Bangladesh
- **Privacy contact:** programmingwitharfin@gmail.com
- **Website:** https://fosterapp.framer.website/
- **This policy:** https://fosterapp.framer.website/privacy
- **Terms & Conditions:** https://fosterapp.framer.website/terms

This policy applies to the Foster mobile apps on Android and iOS, and to information submitted through the public web deletion form. It is publicly accessible on the web (no login needed, no geo-blocking) and is linked inside the app on the sign-in screen and in Settings → Privacy Policy.

**How we notify you of changes:** when we change this policy, we update the "Last updated" date above and, for material changes, show a notice in the app (and in app-release notes) before or when the change takes effect. If a change affects how we use data we already hold in a way that requires a new permission under your law, we will ask for your consent where consent is the lawful basis.

## 1. What Foster does (plain summary)

Foster helps you stay in touch with people who matter to you. You add a person, set how often you want to check in with them, and Foster reminds you. You can write private notes about your check-ins and ask Foster's "brainstorm" feature for conversation ideas based on the notes you wrote. All of this data is **yours alone** — there are no feeds, no follower systems, and no messaging between Foster users.

## 2. What data we collect and why

Everything below is tied to a purpose. "Required" means the feature cannot work without it; you can decline and keep using the rest of the app.

| Data | Purpose | Required / optional |
|---|---|---|
| **Sign-in account data:** your email address, whether it is verified, and your account ID — from Google Sign-In, Apple Sign-In, or your Supabase account | App functionality: creating and securing your account so your data syncs across your devices | Required to create an account. You can decline by not signing in, but then you cannot use the app's saved data |
| **Profile info:** the display name you type, and a profile photo you add | App functionality & personalization: showing who you are in the app | Optional — you can skip the name and use a built-in colored avatar instead |
| **People you track (contact entries):** the name, avatar color, check-in frequency, reminder time, next/last check-in dates, and streak counts you set for each person. To import a person, Foster opens your phone's **built-in contact picker** — we never see your address book; the phone hands us just the name and photo of the one person you tap | App functionality: reminders and check-in tracking | Optional feature — you can also type a name manually |
| **Check-ins and check-in notes** (dates and free-text notes you write) | App functionality: your history and streaks | Optional — required only for the check-in history feature |
| **Private notes about a person** (title and body you type) | App functionality: remembering details for the brainstorm feature | Optional |
| **Custom reminders** (title, description, repeat schedule, date/time) | App functionality: your reminders | Optional |
| **Groups** (names/colors you create, e.g. "Family") | App functionality: organizing people | Optional |
| **Profile avatar image** you add | App functionality & personalization | Optional — built-in colored avatars need no upload |
| **Brainstorm requests:** when you tap brainstorm for a person, the request includes that person's relationship details and the notes you wrote about them, so the AI can suggest conversation topics | App functionality: generating suggestions. See §4 | Optional feature — brainstorm only runs when you tap it |
| **IP address** (processed on our server, see §5) | Fraud/security: rate limiting to stop abuse of the AI feature | Required when you use brainstorm |
| **Purchase and subscription info** (what you bought, entitlement status, an app-user ID that links your Foster account to the purchase) | App functionality: unlocking and restoring your subscription | Required only if you buy a subscription |
| **Local preferences on your device** (onboarding progress, theme, swipe hints, paywall state, notification plan) | App functionality | Optional; stored on your device only |

We do **not** collect: location, health data, browsing history, search history, advertising identifiers, contacts beyond the single contact you choose to import, or any analytics/crash telemetry. We have no advertising SDKs of any kind.

## 3. Sign-in data

Foster uses native Google Sign-In and Apple Sign-In. When you sign in, Google or Apple shares your name and email with us to create your account. With Apple Sign-In you can choose to share a private relay email instead of your real address. Passwords are never seen or stored by us — authentication is handled by Supabase Auth.

## 4. The brainstorm feature and Google Gemini

When you tap brainstorm for a person, our server builds a request from that person's relationship details and the notes you wrote, and sends it to **Google's Gemini API** to generate conversation suggestions. Google processes this text to generate a response. The suggestions are saved as your own data so you can see them later. Only you can trigger a brainstorm for your own contacts, and each person is limited to one brainstorm per day (an anti-abuse limit).

If you prefer that your notes never reach the AI feature, simply do not use brainstorm — it runs only when you tap it, and deleting the notes removes them from what could ever be sent.

## 5. IP address and abuse protection

When your app talks to our server (including edge functions), the network request necessarily carries your IP address. For the brainstorm feature we use the IP to enforce an abuse limit (30 requests per IP per day). The IP is used for this security purpose and is not stored in your account profile or used to profile you. Operator-level security logs may retain request metadata for a short period as part of our hosting provider's standard logging (Supabase/Google Cloud).

## 6. Payments and subscriptions

Subscriptions are sold through Google Play Billing and Apple's App Store, so **we never see your full payment card details** — the store handles payment. Our subscription provider, **RevenueCat**, processes purchase receipts and entitlement data and links it to an app-user ID that corresponds to your Foster account so your subscription can be restored on a new device. RevenueCat's privacy policy: https://www.revenuecat.com/privacy

## 7. Who we share data with

We do **not sell your data, and we do not share it for targeted advertising.** We share data only with these service providers, who process it on our behalf to run the app:

| Provider | Role | What they process | Policy |
|---|---|---|---|
| **Supabase (backed by Google Cloud)** | Hosting, database, authentication, storage, edge functions | Your account, contact entries, notes, check-ins, reminders, avatar images | https://supabase.com/privacy |
| **Google (Gemini API)** | AI text generation for brainstorm | The notes/relationship text you send in a brainstorm request | https://policies.google.com/privacy |
| **RevenueCat** | Subscription management | Purchase/entitlement data, app-user ID, device identifier | https://www.revenuecat.com/privacy |
| **Google Play / Apple App Store** | App distribution, payments | Your store account and transactions | Store privacy policies |
| **Google / Apple** | Sign-in providers | Your name/email (or relay email) at sign-in | Provider privacy policies |
| **Framer** | Website hosting and deletion-request form | Email address and reason submitted through the form | https://www.framer.com/legal/privacy |

## 8. Data retention and deletion

- **In-app deletion (recommended):** Settings → Delete Account → type "DELETE" to confirm. This immediately and permanently deletes your account row, which cascades to delete **all** of your data: profile, people/contacts, groups, check-ins, notes, custom reminders, notification settings, badges, and your avatar files in storage. There is no grace period and no recovery.
- **Web deletion:** you can also request deletion here, without installing the app: https://fosterapp.framer.website/delete-account. Submit the form with the email address associated with your account and a brief reason. We may ask you to confirm ownership of the account before processing.
- **Timing:** in-app deletion is immediate and permanent. Requests submitted through the web form are processed within 30 days. We do not retain backup copies of deleted account data.
- **Legally required retention:** we do not retain your personal data after deletion. Purchase/transaction records may be retained by our subscription processor (RevenueCat) and the app stores where required by tax, accounting, or consumer-protection law; those records are held by the processor, not in your Foster account.
- **Local data on your device** is removed when you delete your account in-app, when you uninstall the app, or via your OS "clear data" controls.

## 9. Security

- All network traffic uses **HTTPS/TLS** (the app contains no cleartext HTTP).
- Access to your data in our database is enforced by row-level security rules tied to your authenticated account ID.
- Account deletion uses server-side verification of your session before deleting anything.
- Your authentication session is stored using your device's standard app-private storage.
- We are a small operation (one developer); if you have questions about our security practices, contact us at the email above.

## 10. Device backups

Foster's local preference files (onboarding drafts, theme, and other settings) are **excluded from Android cloud backup and device-to-device transfer** via backup rules (`backup_rules.xml`, `data_extraction_rules.xml`), so your local app data is not copied to Google's cloud. Authentication sessions likewise stay out of backups — after restoring a device you simply sign in again.

## 11. Children

Foster is a general-audience app **not directed at children under 13** (or under 16 in countries with a higher digital-consent age). We do not knowingly collect personal data from children under 13. If you believe a child has created an account, contact us and we will delete it.

## 12. Advertising and tracking

Foster contains **no advertising SDKs and no tracking**. We do not collect or use the Android Advertising ID or Apple's IDFA, we do not run App Tracking Transparency prompts because we track no one, and we do not sell or share personal data for cross-context behavioral advertising. There is nothing to reset or opt out of — but if you want to limit your device's advertising identifier anyway, use your phone's privacy settings (Android: Settings → Privacy → Ads → Delete advertising ID; iOS: Settings → Privacy → Tracking).

## 13. EEA and UK visitors

Foster is **not distributed in the European Economic Area or the United Kingdom** — it is not offered or targeted to people in those regions. Even so, if you use Foster from there, we voluntarily apply the same standards as this policy describes everywhere else: purpose-limited use of your data, the rights in §§14–16 (access, correction, deletion), immediate in-app deletion and web deletion requests processed within 30 days (§8), and the processor safeguards in §7/§17.

## 14. Your rights (California — CCPA/CPRA)

We do **not sell or share your personal information for cross-context behavioral advertising**, and we have not done so in the last 12 months. We honor Global Privacy Control signals where required. There is no "Limit the Use of My Sensitive Personal Information" link because we collect no sensitive personal information (no precise geolocation, no health, no financial account credentials, no private-communication contents between people).

Categories we collect (with CCPA retention criteria — see §8): **Identifiers** (account ID, app-user ID, device ID — sources: you, RevenueCat); **Personal information categories of Cal. Civ. Code §1798.80** (name, email — source: you/Google/Apple); **Internet or electronic network activity information** (IP-based request processing — source: automatic); **User content** (notes, check-ins — source: you); **Inferences** (none); **Sensitive personal information** (none). Purposes: the purposes in §2; recipients: §7; retention: §8. We do not use or disclose personal information for third-party advertising or to profile you for advertising.

Your rights: **know/access, delete, correct, opt out of sale or sharing (nothing to opt out of — but you may email us to confirm), non-discrimination** (we will never degrade your service for exercising rights). You may use an **authorized agent** with written permission or a power of attorney; we will verify both you and the agent. To exercise rights: email programmingwitharfin@gmail.com, or simply delete your account in-app for immediate erasure. We will verify your identity from your authenticated account session (we never keep enough data to verify you another way — this is a consequence of our data-minimal design) and respond within 45 days.

## 15. Your rights (India — DPDP Act 2023)

You may access, correct, and erase your personal data, and withdraw consent at any time (withdrawal does not affect processing already done). Consent is taken through your clear sign-in and your use of optional features (brainstorm); you can withdraw it by deleting your account or not using the feature. **Grievance officer contact** (also our Consent Manager contact): **Privacy Contact, MD. ARFIN HOSSIN PATWARY — programmingwitharfin@gmail.com**. We will acknowledge grievances within 72 hours and aim to resolve them within 30 days.

## 16. Your rights (Brazil — LGPD)

Legal bases under Brazil's LGPD: performance of our service (contract — providing the app you signed up for), consent (the brainstorm feature — it runs only when you tap it), and legitimate interest (abuse/security protection). We have not appointed a separate DPO; the **Privacy Contact** (programmingwitharfin@gmail.com) acts as our data-protection channel. Your rights: confirmation of processing, access, correction, anonymization/blocking/deletion, portability, information about sharing, information about the possibility of not consenting, and withdrawal of consent. You may complain to the ANPD. Exercising rights is free.

## 17. International transfers summary

Data is stored in Supabase (Google Cloud, us-west-2, United States) and processed by the providers in §7 under their data-processing agreements. Because Foster is not distributed in the EEA/UK, no EU/UK-specific transfer mechanisms are currently relied on; if distribution there ever begins, the Standard Contractual Clauses / UK IDTA already offered by our providers would apply and this policy would be updated.

## 18. How to contact us

Privacy questions, deletion requests, or anything else: **programmingwitharfin@gmail.com**. Postal: MD. ARFIN HOSSIN PATWARY, Taraf Patwary Bari, West Fatehpur, Dagonbhuiyan, Feni - 3900, Bangladesh. We process deletion requests submitted through the web form within 30 days.

---

**Note:** This Privacy Policy is a draft generated from an audit of the app's code. It is not legal advice — have a lawyer review it before publication. The audit appendix backing every claim above is in `STORE_DISCLOSURES.md` (Appendix A).



