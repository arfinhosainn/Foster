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
it in. Gotcha: xcconfig treats `//` as a comment — escape URLs as
`https:$()//…`. Values flow into Info.plist at build time.

Both platforms bootstrap these into `Secrets` at app startup
(`MainActivity.onCreate` / `MainViewController`). `Secrets.configure` is
**hygiene, not secrecy** — the key ships in the binary either way; real data
protection is RLS + rate limits (see [`docs/security-checklist.md`](./docs/security-checklist.md)
for key rotation, the brainstorm abuse defenses, and the full server checklist).

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…