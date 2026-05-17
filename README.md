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

Use Android Studio or command-line Android tooling with these minimum expectations:

- **Android Studio:** Current stable Android Studio release recommended.
- **Android SDK:** Install Android SDK Platform 35 because the app compiles and targets SDK 35.
- **JDK:** JDK 17.
- **Device or emulator:** Android 8.0/API 26 or newer. Test on real devices before considering a release production-ready.
- **Gradle:** Use the checked-in Gradle wrapper; do not require contributors to install a separate Gradle version.

## Fresh clone setup

1. Clone the repository.
2. Open the project in Android Studio, or use the command line from the repository root.
3. If needed, create `local.properties` with your Android SDK path. This file is local-only and must not be committed.
4. Let Gradle sync using the checked-in wrapper.

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
