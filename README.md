This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

## Local secrets setup

This repo is **public** — no credential is ever committed. Builds and the app
fail fast with instructions when secrets are missing. Never commit real values.

**Android** — add to `local.properties` (gitignored; see
[`local.properties.example`](./local.properties.example)):

```
supabase.url=https://<your-project-ref>.supabase.co
supabase.publishable_key=sb_publishable_…
google.web.client.id=<client-id>.apps.googleusercontent.com
revenuecat.android.key=goog_…
```

Gradle resolves each secret as **environment variable first** (same name,
uppercase — e.g. `SUPABASE_PUBLISHABLE_KEY`; this is how CI works without
`local.properties`), then `local.properties`, then fails the build at
configuration time.

**iOS** — copy [`iosApp/Configuration/Secrets.xcconfig.example`](./iosApp/Configuration/Secrets.xcconfig.example)
to `Secrets.xcconfig` (gitignored, auto-included by `Config.xcconfig`) and fill
it in. Gotcha: xcconfig treats `//` as a comment, so write the URL without
adjacent slashes — `https:/$()/your-ref.supabase.co` expands to
`https://your-ref.supabase.co` at build time. Values flow into Info.plist at
build time.

Both platforms bootstrap these into `Secrets` at app startup
(`MainActivity.onCreate` / `MainViewController`). `Secrets.configure` is
**hygiene, not secrecy** — the key ships in the binary either way; real data
protection is RLS + rate limits (see [`docs/security-checklist.md`](./docs/security-checklist.md)
for key rotation, the brainstorm abuse defenses, and the full server checklist).

### RevenueCat dashboard setup

The app expects the following RevenueCat configuration:

- Add the Android package `app.usefoster` and the iOS bundle identifier from
  `iosApp` as separate store apps.
- Add the regular store products to one **current offering**, using
  RevenueCat's `$rc_monthly` and `$rc_annual` package types.
- Optionally add a second offering with identifier `discount`, using the
  discounted annual product for the time-limited discount screen. If it is
  absent, that screen safely falls back to the current offering.
- Attach both products to the `unlimited` entitlement. The app uses this
  entitlement as the only source of premium access.
- Use the public `goog_…` key on Android and `appl_…` key on iOS; never use a
  RevenueCat secret API key in the app.

Prices and trial text are read from the current offering at runtime, so they
must not be duplicated in Kotlin. Purchases are linked to the authenticated
Supabase user UUID after sign-in and returned to an anonymous RevenueCat user
on sign-out.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…