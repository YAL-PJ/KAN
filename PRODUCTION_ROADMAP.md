# KAN Production Roadmap

Use this document as the master checklist for taking KAN from the current GitHub app to a production-quality Google Play release.

## How to use this checklist

- Check items off only after they are verified on a real device or in Play Console.
- Add notes, links, screenshots, and bug IDs under each section as work is completed.
- Every Play Store upload must increment `versionCode` in `app/build.gradle.kts`.
- Prefer testing through Google Play internal testing before calling any release production-ready.

---

## 1. Project setup and clean local build

### Repository basics

- [ ] Add a project `README.md` with app purpose, setup steps, and test instructions.
- [ ] Add the Gradle wrapper files:
  - [ ] `gradlew`
  - [ ] `gradlew.bat`
  - [ ] `gradle/wrapper/gradle-wrapper.jar`
  - [ ] `gradle/wrapper/gradle-wrapper.properties`
- [ ] Confirm a fresh clone builds without manually installing a specific Gradle version.
- [ ] Confirm `.gitignore` excludes local machine files such as `local.properties`, build output, IDE caches, and signing secrets.
- [ ] Document required local tooling:
  - [ ] Android Studio version
  - [ ] Android SDK version
  - [ ] JDK version
  - [ ] Emulator/device requirements

### Clean build checks

- [ ] Run a debug build from a clean clone:

  ```bash
  ./gradlew :app:assembleDebug
  ```

- [ ] Run a release bundle build:

  ```bash
  ./gradlew :app:bundleRelease
  ```

- [ ] Confirm the release bundle exists at:

  ```text
  app/build/outputs/bundle/release/app-release.aab
  ```

- [ ] Run Android lint:

  ```bash
  ./gradlew :app:lintRelease
  ```

- [ ] Fix all release-blocking lint issues.

---

## 2. Versioning and release signing

### Versioning

- [ ] Decide the current release version name.
- [ ] Confirm `versionCode` is higher than the version currently uploaded to Google Play.
- [ ] Confirm `versionName` matches the intended public release label.
- [ ] Add a release note template to the repo.
- [ ] Create a simple versioning rule, for example:
  - [ ] Bug fix release: `1.0.1`, `1.0.2`, etc.
  - [ ] Feature release: `1.1.0`, `1.2.0`, etc.
  - [ ] Major change: `2.0.0`.

### Signing

- [ ] Confirm Google Play App Signing is enabled in Play Console.
- [ ] Confirm who has permission to upload release bundles.
- [ ] Confirm upload key storage is secure and not committed to GitHub.
- [ ] Document the release signing process in a private team note.
- [ ] Test uploading an `.aab` to internal testing.

---

## 3. Core product behavior

### First launch

- [ ] App opens without crashing on a fresh install.
- [ ] App clearly explains what KAN does before asking for sensitive permissions.
- [ ] App explains that tracking uses a foreground service.
- [ ] App explains why the floating pill needs overlay permission.
- [ ] App explains whether data stays on-device or leaves the device.
- [ ] User can skip overlay permission and still use the main app.
- [ ] User can re-open permission setup later from settings.

### Daily screen budget

- [ ] Daily screen-time counter increases while the phone is active.
- [ ] Daily screen-time counter does not increase while the screen is off.
- [ ] Daily budget displays correctly.
- [ ] Budget slider updates the stored budget.
- [ ] Budget value persists after closing and reopening the app.
- [ ] Budget value persists after reboot.
- [ ] Daily budget streak increases when a day finishes under budget.
- [ ] Daily budget streak resets when a day finishes over budget.
- [ ] Midnight/day rollover works correctly.
- [ ] Timezone changes do not corrupt daily history.

### Absence tracking

- [ ] App starts an absence session when the screen turns off.
- [ ] App ends an absence session when the user returns.
- [ ] Last absence session displays correctly.
- [ ] All-time absence record updates only when the new absence is longer.
- [ ] Record notification appears only when notification permission is granted.
- [ ] Absence tracking still works after the app is swiped away.
- [ ] Absence tracking still works after device reboot, when allowed by Android.

### Floating pill overlay

- [ ] Floating pill appears only after overlay permission is granted.
- [ ] Floating pill displays current daily screen time.
- [ ] Floating pill updates once per second while active.
- [ ] Floating pill can be dragged.
- [ ] Floating pill snaps to an edge after dragging.
- [ ] Floating pill position persists after closing and reopening the app.
- [ ] Floating pill position persists after reboot.
- [ ] Floating pill does not block critical system UI.
- [ ] User can reset floating pill position.
- [ ] User can disable floating pill without uninstalling the app.

### Foreground service

- [ ] Persistent notification appears while tracking is active.
- [ ] Notification text clearly explains what is running.
- [ ] Notification tap opens the app.
- [ ] Notification has a useful action, such as opening overlay settings or pausing tracking.
- [ ] User can pause tracking.
- [ ] User can resume tracking.
- [ ] User can stop/reset tracking from app settings.
- [ ] App behaves safely if Android kills and later restarts the service.

---

## 4. App settings and user control

- [ ] Add a settings screen or improve the current settings page.
- [ ] Add pause tracking control.
- [ ] Add resume tracking control.
- [ ] Add reset today's screen time control.
- [ ] Add reset all stats control.
- [ ] Add reset floating pill position control.
- [ ] Add privacy policy link.
- [ ] Add contact/support link.
- [ ] Add app version display.
- [ ] Add permission status display:
  - [ ] Notifications enabled/disabled
  - [ ] Overlay enabled/disabled
  - [ ] Battery optimization status, if relevant
- [ ] Confirm destructive actions show confirmation dialogs.

---

## 5. History and insights

- [ ] Show at least the last 7 days of history.
- [ ] Show each day's screen time.
- [ ] Show each day's budget result.
- [ ] Show each day's best absence duration.
- [ ] Make empty history state understandable.
- [ ] Make history survive app restart.
- [ ] Make history survive device reboot.
- [ ] Decide how many days of history to keep.
- [ ] Add export/delete decision:
  - [ ] If export is supported, document the file format.
  - [ ] If export is not supported, clearly provide delete/reset controls.

---

## 6. Automated tests

### Unit tests

- [ ] Add unit test dependencies.
- [ ] Add tests for clock formatting:
  - [ ] `0` seconds becomes `00:00:00`.
  - [ ] `59` seconds becomes `00:00:59`.
  - [ ] `60` seconds becomes `00:01:00`.
  - [ ] `3600` seconds becomes `01:00:00`.
  - [ ] Negative seconds become `00:00:00`.
- [ ] Add tests for human duration formatting:
  - [ ] Seconds-only duration.
  - [ ] Minutes duration.
  - [ ] Hours and minutes duration.
  - [ ] Negative input.
- [ ] Add tests for budget updates.
- [ ] Add tests for daily rollover.
- [ ] Add tests for budget streak logic.
- [ ] Add tests for history encoding/decoding.
- [ ] Add tests for absence start/finish logic.
- [ ] Add tests for all-time record updates.

### Instrumentation/manual automation

- [ ] Add a basic launch test.
- [ ] Add a settings screen test.
- [ ] Add a budget slider test.
- [ ] Add a permission-denied state test where practical.
- [ ] Add a simple smoke test that opens the app and verifies core UI text.

### Required test commands

- [ ] Unit tests pass:

  ```bash
  ./gradlew test
  ```

- [ ] Android tests pass on a connected device or emulator:

  ```bash
  ./gradlew connectedAndroidTest
  ```

- [ ] Lint passes:

  ```bash
  ./gradlew :app:lintRelease
  ```

---

## 7. Continuous integration

- [ ] Add GitHub Actions workflow for pull requests.
- [ ] CI runs debug build.
- [ ] CI runs unit tests.
- [ ] CI runs lint.
- [ ] CI uploads test/lint reports as artifacts when checks fail.
- [ ] Protect the main branch so failing checks block merges.
- [ ] Add a manual workflow to build release artifacts for internal testing.
- [ ] Make sure CI never prints signing secrets or private keys.

---

## 8. Manual device test matrix

### Devices and Android versions

Test at least these categories before production:

- [ ] Android 8 or 9 device/emulator, because `minSdk` is 26.
- [ ] Android 12 device/emulator.
- [ ] Android 13 device/emulator.
- [ ] Android 14 device/emulator.
- [ ] Android 15 device/emulator.
- [ ] Newest Android version available.
- [ ] Small screen phone.
- [ ] Large screen phone.
- [ ] Device with aggressive battery management, such as Samsung, Xiaomi, Oppo, Vivo, or OnePlus.

### Manual flows

- [ ] Fresh install.
- [ ] First launch.
- [ ] Notification permission allowed.
- [ ] Notification permission denied.
- [ ] Overlay permission allowed.
- [ ] Overlay permission denied.
- [ ] App opened from launcher.
- [ ] App opened from notification.
- [ ] Phone locked for 1 minute.
- [ ] Phone locked for 30 minutes.
- [ ] Phone unlocked after absence.
- [ ] Floating pill dragged.
- [ ] Floating pill disabled.
- [ ] App swiped away from recents.
- [ ] Device reboot.
- [ ] App updated over an older installed version.
- [ ] App data cleared.
- [ ] App uninstalled and reinstalled.
- [ ] Low battery mode enabled.
- [ ] Do Not Disturb enabled.
- [ ] Dark mode enabled.
- [ ] Font size increased.
- [ ] Display size increased.
- [ ] Airplane mode enabled.

---

## 9. Privacy, security, and compliance

### Privacy policy

- [ ] Write and publish a privacy policy.
- [ ] State exactly what data KAN stores.
- [ ] State whether KAN sends any data off-device.
- [ ] State whether KAN uses analytics.
- [ ] State whether KAN uses crash reporting.
- [ ] State whether KAN uses ads.
- [ ] State whether KAN uses accounts or login.
- [ ] State how users can delete their data.
- [ ] Add privacy policy link inside the app.
- [ ] Add privacy policy URL in Play Console.

### Google Play Data Safety

- [ ] Complete the Data Safety form in Play Console.
- [ ] Confirm Data Safety matches the real app behavior.
- [ ] Update Data Safety before adding analytics, crash reporting, ads, login, sync, or network features.
- [ ] Save a copy of the submitted answers for future audits.

### Permissions review

- [ ] Review every manifest permission and confirm it is still needed.
- [ ] Explain notification permission in onboarding.
- [ ] Explain overlay permission in onboarding.
- [ ] Explain foreground service behavior in onboarding.
- [ ] Confirm the app works when notification permission is denied.
- [ ] Confirm the app works when overlay permission is denied.
- [ ] Confirm the app does not ask for unnecessary permissions.

### Foreground service declaration

- [ ] Confirm the current foreground service type is valid for the app's use case.
- [ ] Complete foreground service declaration in Play Console if required.
- [ ] Write a clear explanation of why continuous tracking is user-beneficial.
- [ ] Ensure the service notification is always visible while tracking.
- [ ] Ensure the user can stop or pause tracking.

---

## 10. Play Store listing

- [ ] App name is final.
- [ ] Short description is clear.
- [ ] Full description explains the value of KAN.
- [ ] Screenshots are created from a real or representative phone.
- [ ] Screenshots show:
  - [ ] Main screen.
  - [ ] History/settings screen.
  - [ ] Floating pill.
  - [ ] Permission explanation/onboarding.
- [ ] Feature graphic is created.
- [ ] App icon is final.
- [ ] Category is selected.
- [ ] Tags are selected.
- [ ] Content rating questionnaire is completed.
- [ ] Target audience is completed.
- [ ] Ads declaration is completed.
- [ ] Data Safety is completed.
- [ ] Privacy policy URL is added.
- [ ] Support email is added.
- [ ] Store listing has no claims the app cannot prove.

---

## 11. Internal testing release

- [ ] Build release AAB.
- [ ] Upload AAB to Google Play internal testing.
- [ ] Add yourself as an internal tester.
- [ ] Install app from the Play Store internal test link.
- [ ] Verify Play-installed app launches.
- [ ] Verify permissions from Play-installed app.
- [ ] Verify foreground service from Play-installed app.
- [ ] Verify overlay from Play-installed app.
- [ ] Verify reboot behavior from Play-installed app.
- [ ] Verify an update from one internal version to the next.
- [ ] Collect crash/ANR information from Play Console.
- [ ] Fix all critical internal testing issues.

---

## 12. Closed testing release

- [ ] Recruit a small tester group.
- [ ] Prepare tester instructions.
- [ ] Prepare a feedback form.
- [ ] Upload release to closed testing.
- [ ] Ask testers to test at least 3 days of real use.
- [ ] Ask testers to report device model and Android version.
- [ ] Review crash reports.
- [ ] Review ANRs.
- [ ] Review feedback.
- [ ] Fix high-priority issues.
- [ ] Repeat closed testing if major behavior changes are made.

---

## 13. Production rollout

- [ ] Confirm all required Play Console sections are complete.
- [ ] Confirm no known critical crashes.
- [ ] Confirm no known data loss bugs.
- [ ] Confirm privacy policy and Data Safety are accurate.
- [ ] Confirm release notes are ready.
- [ ] Start production rollout with a small percentage if staged rollout is available.
- [ ] Watch Play Console vitals after rollout.
- [ ] Watch crash rate.
- [ ] Watch ANR rate.
- [ ] Watch user reviews.
- [ ] Pause rollout if a serious issue appears.
- [ ] Increase rollout gradually after metrics look healthy.
- [ ] Document the final production version.

---

## 14. Post-launch maintenance

### Weekly

- [ ] Review Play Console crashes.
- [ ] Review Play Console ANRs.
- [ ] Review user reviews.
- [ ] Review support emails.
- [ ] Triage bugs.

### Monthly

- [ ] Test app on latest Android security update available to you.
- [ ] Check dependency updates.
- [ ] Check Gradle/Android Gradle Plugin updates.
- [ ] Check Compose/library updates.
- [ ] Review privacy policy accuracy.
- [ ] Review Data Safety accuracy.

### Yearly / platform deadline checks

- [ ] Check Google Play target API deadline.
- [ ] Plan target SDK upgrade early.
- [ ] Test new Android behavior changes before the deadline.
- [ ] Update store listing screenshots if UI changed.

---

## 15. Definition of production-ready

KAN is production-ready only when all of these are true:

- [ ] Fresh clone builds successfully.
- [ ] Gradle wrapper is committed.
- [ ] Debug build installs on a real phone.
- [ ] Release AAB builds successfully.
- [ ] Release AAB uploads to Google Play internal testing.
- [ ] App installs from Google Play internal testing.
- [ ] Core tracking works for at least 3 real days.
- [ ] Overlay flow is understandable.
- [ ] Notification flow is understandable.
- [ ] User can pause or stop tracking.
- [ ] User can reset/delete local app data.
- [ ] Unit tests exist and pass.
- [ ] Lint passes.
- [ ] Manual device test matrix is complete.
- [ ] Privacy policy is published.
- [ ] Data Safety is complete and accurate.
- [ ] Foreground service declaration is complete if required.
- [ ] Store listing is complete.
- [ ] Support email works.
- [ ] Crash/ANR monitoring is reviewed.
- [ ] Production rollout is staged and monitored.

