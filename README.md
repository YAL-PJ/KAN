# KAN

KAN is an Android app for tracking daily screen-time budget progress and continuous absence sessions. It keeps the user-facing tracking state visible in the app and, when the user grants overlay permission, in a floating pill.

## Current app behavior

- Shows today's screen time against a daily screen-time budget.
- Tracks the current budget streak.
- Tracks the last absence session and all-time absence record.
- Shows a simple history/settings page with budget adjustment.
- Runs a foreground service so tracking can continue while the main activity is not open.
- Supports an optional floating overlay pill after the user grants overlay permission.
- Restarts tracking after device boot or app update when Android allows it.

## Requirements

Use GitHub Actions for cloud builds, or command-line Android tooling locally:

- **GitHub account:** Required to run the hosted build workflow and download artifacts.
- **Android SDK (local only):** Install Android SDK Platform 35 because the app compiles and targets SDK 35.
- **JDK (local only):** JDK 17.
- **Device:** Android 8.0/API 26 or newer for installation tests.
- **Gradle:** Use the checked-in Gradle wrapper; do not require contributors to install a separate Gradle version.

## Fresh clone setup

1. Clone the repository.
2. Open the project in Android Studio, or use the command line from the repository root.
3. If needed, create `local.properties` with your Android SDK path. This file is local-only and must not be committed.
4. Let Gradle sync using the checked-in wrapper.


## Interactive design with Compose Preview

For design iteration, open the project in Android Studio and use the Compose Preview tool window instead of rebuilding and reinstalling the APK after every small UI change. The app includes preview fixtures and `@Preview` entry points for the main app shell, main hub, history/settings, onboarding, and lock-timer screens.

1. Open this repository in Android Studio and let Gradle sync.
2. Open a screen file such as `app/src/main/java/com/kan/app/ui/screens/MainHubScreen.kt`.
3. Click **Split** or **Design** to show Compose Preview.
4. Edit colors, spacing, text, or layout in the Compose files and click **Refresh** if Android Studio does not update automatically.
5. Use the sample state in `app/src/main/java/com/kan/app/ui/preview/PreviewFixtures.kt` when a screen needs realistic timer/history data.

Useful preview files:

- `MainHubScreen.kt`: primary dashboard and missing-overlay-permission states.
- `HistorySettingsScreen.kt`: populated and empty ledger states.
- `OnboardingScreen.kt`: first onboarding step.
- `LockTimerScreen.kt`: away timer and active challenge states.
- `KanApp.kt`: full app shell for completed onboarding and first-run onboarding states.

## Phone-only build flow (no Android Studio)

If you want a no-Android-Studio flow, use GitHub Actions to compile and then install from your phone:

1. **LLM pushes code** to this repository branch.
2. **GitHub compiles the app** automatically with the `Build Debug APK` workflow.
3. On your phone, open the repository in a browser and tap **Actions**.
4. Open the latest `Build Debug APK` run and wait for the yellow running icon to become a green check.
5. Scroll to **Artifacts** and tap **debug-apk** to download the artifact zip.
6. Extract the zip and install `app-debug.apk` on your phone.

Notes:
- If Android blocks installation, enable install from browser/files for the app you used to open the APK.
- This workflow is also available manually via **Actions → Build Debug APK → Run workflow**.

## Build and test commands

Run these commands from the repository root.

### Debug build

```bash
./gradlew :app:assembleDebug
```

### Release bundle

```bash
./gradlew :app:bundleRelease
```

The release Android App Bundle should be created at:

```text
app/build/outputs/bundle/release/app-release.aab
```

### Release lint

```bash
./gradlew :app:lintRelease
```

### Unit tests

```bash
./gradlew test
```

### Connected Android tests

Requires a connected Android device or running emulator:

```bash
./gradlew connectedAndroidTest
```

## Release notes and versioning

Every Google Play upload must increment `versionCode` in `app/build.gradle.kts`.

Recommended versioning rule:

- Bug fix release: `1.0.1`, `1.0.2`, etc.
- Feature release: `1.1.0`, `1.2.0`, etc.
- Major change: `2.0.0`.

Release note template:

```markdown
## KAN <versionName>

### Added
- 

### Changed
- 

### Fixed
- 

### Testing
- Debug build:
- Release bundle:
- Lint:
- Device/manual checks:
```

## Privacy and permissions notes

KAN currently declares permissions for overlay display, foreground tracking, notifications, boot restart handling, and wake locks. Before a Play release, verify every permission is still required and make sure onboarding explains notification, overlay, and foreground service behavior before requesting sensitive permissions.

## Production readiness reminder

Do not call KAN production-ready until the roadmap checks are complete, including a fresh clone build, release bundle build, lint, real-device testing, Google Play internal testing, privacy policy, accurate Data Safety answers, and a staged production rollout plan.
