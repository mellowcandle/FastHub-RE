# FastHub-RE Revival — Journal

Working log and roadmap for bringing FastHub-RE back to life.

- **Base:** [`LightDestory/FastHub-RE`](https://github.com/LightDestory/FastHub-RE) @ `caec31c` (master)
- **Started:** 2026-09-04
- **Branch:** `revival`

---

## Why this base

Chosen over the original `k0shk0sh/FastHub` (archived Sept 2022) because the two hardest
migrations are already done and shipped here:

| | k0shk0sh/FastHub | FastHub-RE (this) |
|---|---|---|
| Language | 87% Java (2.23 MB Java / 326 KB Kotlin) | **99% Kotlin** — 784 `.kt`, 71k lines, 4 `.java` |
| Database | requery (annotation processor, build blocker) | **ObjectBox 3.1.2** |
| Build | Gradle 3.5 / AS 4.1 era | Gradle 7.4.1, AGP 7.2.1, Kotlin 1.6.10 |
| Blobs | Firebase + analytics | stripped |

Dead ends checked and ruled out:
- `k0shk0sh/FastHub` branch `v5` — abandoned ground-up rewrite, last commit **Jan 2020**.
- `zeromake/FastHub` — **archived**; its Kotlin/ObjectBox work is already merged here.

Worth mining: [`timscriptov/FastHub-RE`](https://github.com/timscriptov/FastHub-RE) is **3 commits
ahead** (Oct 2023) — Gradle version catalog (`libs.versions.toml`), dependency bumps
(Kotlin 1.9.10, appcompat 1.6.1, material 1.10.0, okhttp 4.11.0, Apollo 4.0.0-beta.1),
and a Material 3 migration.

## State of the upstream project

- Last **code** commit: **2022-07-23** (ObjectBox migration, #46). Over four years cold.
- Only commit since: 2025-10-28, fastlane metadata so IzzyOnDroid can package the 2022 build.
- Last release: **4.7.7, 2 April 2022**.
- README carries a maintainer banner: *"This project is on hold... any PR and bug report will be still accepted."* — so upstreaming fixes is plausible.
- 32 open issues, several unanswered bugs from 2023–2024.

---

## Phase 0 — Get a green baseline

Do **not** change any dependency until an unmodified build runs on a device. Otherwise a bug
you introduce is indistinguishable from a bug that was always there.

- [x] **Fix Windows-only paths** — `app/build.gradle.kts:17,55,58` hardcode `\\app\\keys_debug.jks`
      and `\\app\\secrets.properties`. Nothing builds on Linux until these use `/`.
- [x] **Install Android SDK** — none present (`ANDROID_HOME` unset). Needs `compileSdk 31`
      + `buildToolsVersion 31.0.0` to match the current config.
- [x] **Point Gradle at JDK 11** — AGP 7.2.1 predates JDK 17 support; system default is 17.
      JDK 11 is installed at `/usr/lib/jvm/java-11-openjdk-amd64`.
- [x] **`./gradlew assembleDebug`** — first green build. ✅ 2026-09-04, 5m42s, 22 MB APK.
- [ ] **Install on a device and log in.**

### The single biggest unknown: OAuth

`app/build.gradle.kts:13-16` hardcodes demo credentials (`GITHUB_CLIENT_ID`, `GITHUB_SECRET`,
plus Imgur). They are four years old and public. **If GitHub has revoked that client, login is
dead on arrival and nothing else in this plan matters.** Test before writing any code.

Relevant: `helper/GithubConfigHelper.kt`, `provider/rest/LoginProvider.kt`,
`ui/modules/login/LoginActivity.kt`, `ui/modules/login/LoginPresenter.kt`.

Fallback: register a personal OAuth app, put real values in `app/secrets.properties`
(gitignored). Note the loader at `app/build.gradle.kts:17` splits on `=` with
`it.split("=")` — a secret containing `=` will be silently truncated. Worth hardening.

---

## Phase 1 — Build system (low risk, unblocks everything)

- [ ] Cherry-pick / re-apply timscriptov's version catalog → `gradle/libs.versions.toml`.
- [ ] Gradle wrapper 7.4.1 → 8.x.
- [ ] AGP 7.2.1 → 8.x (namespace already set, so this is less painful than usual).
- [ ] Kotlin 1.6.10 → 2.x. Expect `kapt` → **KSP** migration pressure.
- [ ] Java 8 → 17 target; move off `enableJetifier` if nothing still needs it.
- [ ] Fix CI: `.github/workflows/android_build.yml` uses `actions/checkout@v3`,
      `setup-java@v3`, `upload-artifact@v2`, and the **archived** `actions/create-release@v1`
      and `upload-release-asset@v1.0.1`. Replace the release steps with `gh release create`.

Verify after each step: `./gradlew assembleDebug` still green.

---

## Phase 2 — Platform catch-up (targetSdk 31 → current)

Four platform generations of behavior changes. Ship these one at a time, testing on a modern
device between each.

- [ ] `compileSdk`/`targetSdk` 31 → current (`app/build.gradle.kts:34,36`).
- [ ] **Android 13** — `POST_NOTIFICATIONS` runtime permission. The whole notification path
      needs a permission request; currently there is none in `AndroidManifest.xml`.
- [ ] **Android 14** — `foregroundServiceType` now mandatory. Manifest declares
      `FOREGROUND_SERVICE` with no type.
- [ ] **Android 12+** — `PendingIntent` mutability flags; exact-alarm restrictions.
- [ ] Audit `android:exported` on the 4 exported components (manifest lines 62, 77, 290, 329).
- [ ] **Notifications:** `provider/tasks/notification/NotificationSchedulerJobTask.kt` uses raw
      `JobScheduler`. Consider WorkManager — more robust across OEM battery optimizations.
- [ ] Drop `READ_PHONE_STATE` if unused — it triggers store/privacy review friction.
- [ ] Predictive back gesture.

---

## Phase 3 — Dependency debt

Ordered by risk. The top two are the real work.

- [ ] **RxJava 2 → RxJava 3 or coroutines — 92 files.** RxJava 2 is EOL. Biggest single item;
      do it incrementally, package by package.
- [ ] **ThirtyInch MVP framework — archived March 2021** (`GCX-HCI/ThirtyInch`). It's the spine
      of the presenter layer, but only **10 files** import it directly, so replacement is more
      tractable than it first appears.
- [ ] **Butterknife-style view binding — 39 files** → ViewBinding.
- [ ] Apollo 3.1.0 → 4.x (GraphQL; 3 `.graphql` files).
- [ ] `com.evernote:android-state` — dead; → `SavedStateHandle`.
- [ ] `com.atlassian.commonmark` — renamed to `org.commonmark` years ago; 6 artifacts to move.
- [ ] Unmaintained JitPack deps, each a potential build-breaker if the repo vanishes:
      `Toasty`, `HtmlSpanner`, `colorpicker`, `shortbread`, `ShapedImageView`,
      `sephiroth bottom-navigation`, `material-about-library`, `RetainedDateTimePickers`.
- [ ] Remove the aliyun mirrors in `build.gradle.kts` — irrelevant outside CN, and extra
      supply-chain surface.
- [ ] Glide 4.13.1, OkHttp 4.9.3, Retrofit 2.9.0 → current.

---

## Phase 4 — Make it a real project again

- [ ] Register **own** GitHub OAuth app; stop shipping shared credentials.
- [ ] Decide on the "unlock PRO features" behavior inherited from FastHub-Libre — it's the
      thing upstream objected to. Matters if this is ever published under your name.
- [ ] Rename / rebrand? `applicationId` is `com.fastaccess.github.revival`. Changing it means
      existing users can't upgrade in place.
- [ ] Triage the 32 inherited open issues; several are reproducible bugs with real reports
      (#41 themes, #50 unreadable background, #54 dark theme, #59 trending crash,
      #60 trending → repo).
- [ ] Set up F-Droid / IzzyOnDroid publishing — `fastlane/` metadata already exists.

---

## Known landmines

- `app/build.gradle.kts` uses **Windows path separators** throughout — assume anything
  filesystem-related in the build was only ever tested on Windows.
- Release signing expects `app/keys_release.jks`, not in the repo (correctly). Only
  `keys_debug.jks` is committed.
- README's own warning from the previous maintainer: *"most of the stuff is deprecated or so
  stuck together that if you mess something it will be a pain to stacktrace the error."*
- `minSdk 25` (Android 7.1) — raising it would remove a lot of compat code, at the cost of
  old devices.

---

## Log

### 2026-09-04 — Session 1

- Surveyed the fork landscape; picked `LightDestory/FastHub-RE` as the base (rationale above).
- Unshallowed the clone — full history restored, **1972 commits**, tags back to `4.7.2`.
- Created branch `revival`.
- Wrote this journal.
- **Fixed the Windows path separators** in `app/build.gradle.kts` (3 sites) — the hard blocker
  for building on Linux/macOS.
- **Installed Android SDK** at `~/Android/Sdk`: cmdline-tools, `platforms;android-31`,
  `build-tools;31.0.0`, platform-tools. `local.properties` written (already gitignored).
- **🎉 BASELINE IS GREEN.** `./gradlew assembleDebug` with `JAVA_HOME=java-11-openjdk-amd64`
  succeeded in 5m42s. Output: `app/build/outputs/apk/debug/app-debug.apk`, 22 MB,
  `com.fastaccess.github.revival.debug`, versionName `4.7.7-debug`, minSdk 25, targetSdk 31.
  The 2022 code still compiles unmodified — the project is genuinely revivable.
- Build is warning-heavy but clean: lots of `Deprecated in Java` on `FragmentStatePagerAdapter`,
  `setHasOptionsMenu`/`onCreateOptionsMenu`, `setUserVisibleHint`, `systemUiVisibility`,
  `Html.fromHtml`, `defaultDisplay`. All expected for the era; all on the Phase 2/3 list.
- Noted: `ThemeFragment.kt:96-97` — **"Duplicate label in when"**. That's a real latent bug,
  not just deprecation, and it sits in the theming code that issues #41 and #54 complain about.
  Worth a look early.

**Environment for future sessions:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

**Blocked on hardware:** no KVM / no virtualization extensions on this machine, so no emulator.
The OAuth smoke test (Phase 0's real unknown) needs a physical device over `adb`.
