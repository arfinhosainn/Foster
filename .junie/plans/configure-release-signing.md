---
sessionId: session-260906-173249-1c1g
---

# Requirements

### Overview & Goals
Configure a reproducible, signed Android release build for Google Play testing while keeping the keystore and all signing credentials outside the public repository. Work incrementally so each step can be completed and checked before continuing.

### Scope
**In scope**
- Create the release keystore at `/Users/arfinhossin/Desktop/keystore`.
- Configure the `app.usefoster` Android release variant to use that keystore.
- Keep the keystore, passwords, and machine-specific paths out of Git.
- Build and verify a signed release artifact, preferably an `.aab` for Play Console.
- Document the local setup and the exact next Play Console upload step.

**Out of scope**
- Uploading the artifact to Google Play on the user’s behalf.
- Adding RevenueCat products or changing subscription configuration.
- Committing any keystore, password, signing certificate private key, or local machine path.

### Acceptance Criteria
- The keystore exists under `/Users/arfinhossin/Desktop/keystore` and is not inside the repository.
- `androidApp/build.gradle.kts` signs only the release build with the configured keystore.
- Debug builds remain usable without release signing credentials.
- Release signing values are supplied through ignored local configuration or environment variables.
- `./gradlew :androidApp:bundleRelease` produces a signed Play-uploadable bundle.
- The generated artifact’s signing status is verified without exposing passwords or private material.

# Technical Design

### Current Implementation
- `androidApp/build.gradle.kts` defines `applicationId = "app.usefoster"`, `versionCode = 5` for the next Play upload because version codes 1, 2, 3, and 4 are already used, and a minified `release` build type, but has no `signingConfigs` block or release signing assignment.
- The same file already resolves build secrets from environment variables first and gitignored `local.properties` second; this pattern should be extended for signing values without placing them in source.
- `.gitignore` already excludes `local.properties`, `*.jks`, and `*.keystore`, and `docs/security-checklist.md` explicitly requires signing artifacts to remain uncommitted.
- `README.md` currently documents local secrets but not Android release signing.

### Key Decisions
- **Keystore location:** use `/Users/arfinhossin/Desktop/keystore/usefoster-release.jks`, outside the repository as requested.
- **Credential resolution:** support environment variables for CI and ignored `local.properties` entries for local development; never hardcode passwords in `androidApp/build.gradle.kts`.
- **Release-only signing:** attach the signing configuration to `buildTypes.release` while leaving debug builds independent of the release keystore.
- **Artifact:** use `bundleRelease` as the primary validation target because Google Play distribution uses Android App Bundles; optionally produce a signed release APK only when a direct-install artifact is needed.
- **Versioning:** use `versionCode = 5` for the next upload because version codes 1, 2, 3, and 4 are already used; increase it for every subsequent Play upload.

### Proposed Changes
1. Add release-signing value resolution in `androidApp/build.gradle.kts` for the keystore path, alias, store password, and key password, with a clear release-build error when a required value is missing.
2. Create `signingConfigs.release` and assign it to `buildTypes.release`; avoid evaluating missing signing secrets for debug-only tasks.
3. Extend `local.properties.example` with placeholder signing entries and environment-variable names, without adding real values.
4. Add a concise local signing section to `README.md` describing the desktop keystore path, ignored configuration, `keytool` creation, and Play bundle command.
5. Generate the keystore interactively in the desktop `keystore` folder using a non-exported password; the password will not be requested in chat or written into the repository.

### Local Configuration Contract
Use either environment variables or ignored `local.properties` values:

- `ANDROID_KEYSTORE_PATH` / `android.keystore.path`
- `ANDROID_KEY_ALIAS` / `android.keystore.alias`
- `ANDROID_KEYSTORE_PASSWORD` / `android.keystore.password`
- `ANDROID_KEY_PASSWORD` / `android.key.password`

The local path should resolve to `/Users/arfinhossin/Desktop/keystore/usefoster-release.jks`. Passwords should be entered locally and protected with appropriate file permissions.

### File Structure
- Modify `androidApp/build.gradle.kts` for release signing resolution and `signingConfigs` wiring.
- Modify `local.properties.example` with non-secret placeholders.
- Modify `README.md` with the step-by-step signing procedure and artifact location.
- Create `/Users/arfinhossin/Desktop/keystore/usefoster-release.jks` outside the repository; do not add it to project files.

### Risks and Mitigations
- **Lost keystore:** back it up securely; the same signing key is required for future Play updates.
- **Secret leakage:** keep passwords in ignored local configuration or environment variables and never paste them into source, logs, or chat.
- **Debug breakage:** defer strict signing validation to release configuration/tasks so normal debug development does not require the keystore.
- **Play rejection:** verify the bundle is signed and increment `versionCode` for later uploads.

# Testing

### Validation Approach
- Confirm the keystore path is outside the repository and the file is ignored/not tracked.
- Run an existing debug build to verify release signing setup does not break debug development.
- Run `./gradlew :androidApp:bundleRelease` through the project’s Gradle validation procedure.
- Inspect the generated `.aab` signing metadata with Android tooling without printing credentials.
- Run `git diff --check` and confirm no keystore, password, or generated release artifact is staged or tracked.

### Key Scenarios
- Debug build succeeds when release signing values are absent.
- Release bundle fails with a clear configuration message when a signing value is missing.
- Release bundle succeeds when the desktop keystore and local credentials are configured.
- The resulting bundle has application ID `app.usefoster` and is signed by the newly created release key.

### Edge Cases
- Keystore path does not exist or points to a directory: fail with an actionable message.
- Alias or password is incorrect: report the Gradle/keytool failure without exposing the secret.
- A second build reuses the same keystore and produces a valid artifact.
- A repository status check shows no signing material or local secret file added.

# Delivery Steps

### ✓ Step 1: Confirm release-signing inputs and guardrails
The release-signing contract is fixed without exposing any secret values.

- Confirm the target file name under `/Users/arfinhossin/Desktop/keystore` and the key alias to use.
- Confirm that the existing `app.usefoster` application ID remains unchanged and that the next upload uses `versionCode = 5` because version codes 1, 2, 3, and 4 have already been used.
- Verify the keystore will stay outside `/Users/arfinhossin/Downloads/Nekko` and that `.gitignore` protections remain in place.
- Do not collect or store keystore passwords in chat.

### * Step 2: Create the desktop release keystore
A new Android release keystore exists at `/Users/arfinhossin/Desktop/keystore/usefoster-release.jks`.

- Create the `keystore` directory on the Desktop if needed.
- Generate the keystore interactively with `keytool`, using a stable alias and locally entered passwords.
- Record the alias and file location in local configuration only; keep passwords out of source control and conversation history.
- Preserve the keystore as the long-term signing identity and recommend a secure backup before Play upload.

###   Step 3: Wire release signing into the Android module
The `release` build type in `androidApp/build.gradle.kts` uses the external keystore while debug builds remain independent.

- Add environment/local-property resolution for keystore path, alias, store password, and key password.
- Add `signingConfigs.release` and assign it only to `buildTypes.release`.
- Ensure missing release credentials produce an actionable release configuration error without breaking debug builds.
- Extend `local.properties.example` and `README.md` with placeholders and the safe local setup procedure, never real credentials.

###   Step 4: Build and verify the Play bundle
A signed `.aab` for `app.usefoster` is generated and verified without exposing signing secrets.

- Configure the local signing values using the desktop keystore path and locally stored passwords.
- Run the debug build as a regression check, then run `./gradlew :androidApp:bundleRelease`.
- Inspect the generated bundle’s signing metadata and confirm the artifact location for Play Console internal testing.
- Confirm `git diff --check`, repository status, and ignored-file behavior show no keystore, password, or generated artifact added to the repository.
- Explain the next manual action: upload the bundle to Google Play Internal testing, then install it from the Play testing track before testing RevenueCat purchases.