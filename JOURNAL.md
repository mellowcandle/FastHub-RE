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
- [x] **Provide a real JDK** — the machine had only JREs (no `javac` anywhere), which
      AGP 7 tolerated but AGP 8's toolchain check rejects. Temurin **21** installed to
      `~/jdks/jdk-21.0.12.1+1` (no sudo needed).
- [x] **`./gradlew assembleDebug`** — first green build. ✅ 2026-09-04, 5m42s, 22 MB APK.
- [x] **Installed and launched on a device** (OnePlus 7, Android 12 / API 31) — 2026-09-04.
      No crash, login chooser renders correctly. Full login still untested (needs a human).

### The OAuth credentials: ✅ STILL VALID (verified 2026-09-04)

The hardcoded demo credentials in `app/build.gradle.kts` still work. Verified
against GitHub's token-exchange endpoint, which validates the client id/secret
pair immediately:

| client_id + secret | GitHub response |
|---|---|
| the app's real pair | `bad_verification_code` — pair accepted, only the (deliberately fake) code rejected |
| a bogus control pair | `Not Found` |

So the OAuth app registered by the FastHub-RE maintainer is still live and the
revival is not dead on arrival.

**A test that looked convincing and was not:** hitting
`/login/oauth/authorize` with the real client_id returns `302 → /login`, which
reads like success. A bogus client_id returns *the same 302* — GitHub defers
client validation until after sign-in. Only the token endpoint distinguishes
them. Don't re-derive this the hard way.

Caveats that remain:
- These are **shared, public** credentials that anyone can extract from the
  repo, and they can be revoked at any time without warning. Registering a
  personal OAuth app is still Phase 4 work.
- What is proven is that the *credentials* are accepted. An end-to-end login
  (authorize → code → token → API call) has not been run.

Relevant: `helper/GithubConfigHelper.kt`, `provider/rest/LoginProvider.kt`,
`ui/modules/login/LoginPresenter.kt` (builds the authorize URL, scopes
`user,repo,gist,notifications,read:org,workflow,read:packages`).

---

## Phase 1 — Build system ✅ DONE (2026-09-04)

- [x] Version catalog → `gradle/libs.versions.toml` (63 libraries, 44 version refs).
- [x] Gradle wrapper 7.4.1 → **8.14.5**.
- [x] AGP 7.2.1 → **8.13.2**.
- [x] Kotlin 1.6.10 → **2.2.21**; migrated to the `compilerOptions` DSL.
- [x] Java 8 → **17**. (`enableJetifier` must stay — see below.)
- [x] CI rewritten: maintained actions, JDK 21, `gh release create`.
- [x] ObjectBox 3.1.2 → 3.8.0, Apollo 3.1.0 → 3.8.6, compileSdk 31 → **36**.

`kapt` still works under K2 (android-state, ObjectBox), so the KSP migration
is optional rather than forced. Left for later.

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
- [x] ~~Apollo 3.1.0 → 3.8.6~~ done in Phase 1. Still worth going to 4.x/5.x later,
      which is a breaking API change, not just a version bump.
- [ ] **Replace `RetainedDateTimePickers`** — the last pre-AndroidX dependency and the
      only thing keeping Jetifier on. Highest-value item in this phase.
- [ ] `material-about-library` 2.1.0 → 3.1.2 — needs the theme attributes sorted first.
- [ ] `com.evernote:android-state` — dead; → `SavedStateHandle`.
- [ ] `com.atlassian.commonmark` — renamed to `org.commonmark` years ago; 6 artifacts to move.
- [ ] Unmaintained JitPack deps, each a potential build-breaker if the repo vanishes:
      `Toasty`, `HtmlSpanner`, `colorpicker`, `shortbread`, `ShapedImageView`,
      `sephiroth bottom-navigation`, `material-about-library`, `RetainedDateTimePickers`.
      All now resolve through `libs.versions.toml`, so swapping one is a single edit.
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
- [x] **#59 (trending crash) and #60 (can't open repo from trending) — FIXED**, one root
      cause, verified on device. See log below.
- [ ] Triage the rest of the 32 inherited open issues (#41 themes, #50 unreadable
      background, #54 dark theme). Worth reporting the trending fix upstream — the
      maintainer said PRs are still accepted.
- [ ] Set up F-Droid / IzzyOnDroid publishing — `fastlane/` metadata already exists.

---

## Known landmines

### ⚠️ This machine crashed the JVM twice (suspect RAM)

Two `SIGSEGV`s in the **C2 JIT compiler thread**, on two different JDKs:

| JDK | Crashing frame |
|---|---|
| Temurin 17.0.20.1 | `PhaseOutput::fill_buffer` |
| Temurin 21.0.12.1 | `PhaseCFG::fixup_flow` |

Different JDK majors crashing at different points inside the JIT is not a
toolchain bug. The second faulting address was `0x00001000007f953e` — what
should be a `0x00007f95...` pointer with a stray high bit set, the signature of
a single-bit memory error. The host is an i7-2600K (2011).

**Both crashes were transient — retrying the identical build succeeded.** But
treat any one-off build failure here as suspect before believing it, and
consider running `memtest86+` overnight. If the RAM is bad, it corrupts
compiler output silently as well as loudly.

### Jetifier cannot be removed yet

`android.enableJetifier=true` is still load-bearing. Exactly one dependency
still drags in the pre-AndroidX support library:

```
com.android.support:support-compat:25.1.0
  └── com.android.support:support-fragment:25.1.0
      └── com.github.k0shk0sh:RetainedDateTimePickers:1.0.2
```

That library is by FastHub's original author, dates from 2017, and has no
AndroidX release. **Replacing it (Material date/time pickers) is what unblocks
dropping Jetifier** — worth it, since Jetifier rewrites every dependency on
every build.

(`material-about-library` was the other offender and 3.1.2 fixes its half, but
see below.)

### material-about-library 3.x needs theme work

Bumping 2.1.0 → 3.1.2 compiles but **fails resource linking**: the app's own
styles reference `attr/mal_popupOverlay`, `attr/mal_lightActionBar` and
`style/Theme.Mal.{Dark,Light}.PopupOverlay`, which 3.x removed. Reverted for
now — it is a themed-UI change that needs to be looked at on a real screen, not
just compiled.

### Other

- Release signing expects `app/keys_release.jks`, not in the repo (correctly).
  Only `keys_debug.jks` is committed.
- README's warning from the previous maintainer: *"most of the stuff is
  deprecated or so stuck together that if you mess something it will be a pain
  to stacktrace the error."* So far this has not been borne out — the upgrade
  ladder went through cleanly.
- `minSdk 25` (Android 7.1) — raising it would remove a lot of compat code, at
  the cost of old devices.

---

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

### 2026-09-04 — Session 2 (autonomous): Phase 1 complete

Worked the upgrade ladder on branch `phase1-build-system`, building green at every
rung. Seven commits, `b8a9fa6..a83b439`.

| | Before | After |
|---|---|---|
| Gradle | 7.4.1 | **8.14.5** |
| AGP | 7.2.1 | **8.13.2** |
| Kotlin | 1.6.10 | **2.2.21** |
| Java | 8 | **17** |
| compileSdk | 31 | **36** |
| ObjectBox | 3.1.2 | **3.8.0** |
| Apollo | 3.1.0 | **3.8.6** |
| targetSdk | 31 | **31** (deliberately unchanged — Phase 2) |

**The upgrades were interlocked, each forcing the next:** ObjectBox had to move
first because AGP 8 removed the Transform API it used; ObjectBox 3.8 pulls
kotlin-stdlib 1.8.20, which collided with the pinned `kotlin-stdlib-jdk8:1.6.10`
(merged into the main stdlib in Kotlin 1.8) — forcing Kotlin 1.9; under Kotlin 1.9
the Apollo 3.1.0 plugin stopped putting generated sources on the compile classpath
(263 unresolved references) — forcing Apollo 3.8.6.

**AGP 8 broke 16 source files, all legitimately:**
- `android.nonTransitiveRClass` now defaults true, so the app's `R` stopped
  re-exporting library resources. Six references repointed at the owning library
  (`androidx.appcompat`, `com.google.android.material`,
  `com.danielstone.materialaboutlibrary`) rather than setting the compatibility
  flag, which AGP 9 removes anyway.
- compileSdk 36 tightened platform nullability: `MenuItem.getIcon()`,
  `PackageInfo.applicationInfo`, `onDraw(Canvas)`, `Canvas.getClipBounds(Rect)`,
  `AnimatorListener.onAnimationEnd`.

**Also fixed along the way:**
- CI was not merely stale but *incapable of running* — `actions/create-release@v1`
  and `upload-release-asset@v1.0.1` are archived and `upload-artifact@v2` is
  disabled by GitHub. Rewritten around `gh release create`.
- `loadConfig` printed "Secrets found!" *before* reading the file (every build
  logged both found and not-found) and split on every `=`, truncating any secret
  containing one — which base64 and signing values routinely do.
- Removed a dead `registerForActivityResult` block in `ThemeFragment` whose `when`
  had three branches all matching the literal `"placeholder"` — leftover from the
  in-app-purchase removal. Nothing ever launched it.

**Not done, and why:**
- `targetSdk` stays 31. Bumping it opts into four generations of runtime behaviour
  changes that need a device to validate. That is Phase 2 and it is blocked on
  hardware, not on effort.
- Nothing here has been **run**. Everything is compile-verified only. The OAuth
  smoke test from Phase 0 is still the open question that governs whether any of
  this matters.

**Build environment:**
```bash
export JAVA_HOME=$HOME/jdks/jdk-21.0.12.1+1
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug     # ~1m30s from scratch, 22 MB APK
```

### Next up

1. **Install the APK and log in** (needs the phone) — still the decisive test.
2. Phase 2: `targetSdk` → 36, one behaviour change at a time, device-verified.
3. Phase 3: replace `RetainedDateTimePickers` to drop Jetifier.

### 2026-09-04 — Session 3: on-device verification

Phone connected (OnePlus 7 / GM1903, Android 12, API 31), `adb` authorized with
no udev rules needed.

- **The modernized build runs.** Installed the AGP 8 / Kotlin 2.2 / compileSdk 36
  APK; it launches to `LoginChooserActivity` with no crash and renders correctly.
  Phase 1 is now runtime-verified, not just compile-verified.
- **OAuth credentials confirmed valid** (see Phase 0 above). The single biggest
  risk to the whole revival is retired.

Note on this device: it is **API 31**, the same as our `targetSdk`. It therefore
cannot exercise any of the Phase 2 behaviour changes — `POST_NOTIFICATIONS`
(API 33), foreground service types (34), predictive back. Validating the
targetSdk bump needs a newer device, or the emulator (which needs VT-x enabled
in this machine's BIOS).

Still not done: an actual end-to-end login. That needs a human to type
credentials — either the ACCESS TOKEN path (recommended by the app itself) or
the browser OAuth path, which is currently blocked behind Chrome's unfinished
first-run screen on this phone.

### 2026-09-04 — Session 4: logged in, first real bug fixed

**End-to-end verified.** Authorized via OAuth on the device; the app reached
MainActivity and rendered a live feed with current data, avatars and parsed
timestamps. That exercises the whole chain — token exchange, REST calls,
Gson/Retrofit parsing, Glide — on AGP 8.13 / Kotlin 2.2 / compileSdk 36 against
today's GitHub API. Issues and Pull Requests tabs also load real data.

**Fixed #59 + #60 — one root cause.** GitHub moved the repo name on `/trending`
from `<h1>` to `<h2 class="h3 lh-condensed">`. `TrendingFragmentPresenter`
still selected `"h1 > a"`, so every row parsed a **blank title**:

- Rows rendered with description/stars/language but no `owner / repo` heading (#60).
- `onItemClick` did `title.split("/")` and indexed `[1]`; on a blank title that
  list has one element → `IndexOutOfBoundsException: Index: 1, Size: 1` (#59).
  Reproduced on device before fixing.

Fix: accept `"h1 > a, h2 > a"`, and guard the click handler so a future markup
change degrades to an inert tap instead of a crash. Verified: names render and
tapping opens the repo with README, stars, forks and license.

**Worth noting for the roadmap:** this is the failure mode to expect from the
scraped-HTML features generally. Trending has no API; it will break again. The
guard means the next break is cosmetic rather than a crash.

**On the OAuth app:** the consent screen shows the credentials belong to
*"Demo FastHub" by Kosh Alsirjani* — the original FastHub author, not the
FastHub-RE maintainer — requesting private repos, gist write, and workflow
scope. Since the client secret is public in this repo, another app could
register the `fasthub://login` scheme and exchange an intercepted code.
Registering a personal OAuth app (Phase 4) is a security fix, not just hygiene.
