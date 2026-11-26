# Flip Launcher (KaiOS-style Android launcher)

`com.flipos.launcher` — minSdk 21, targetSdk 34, compileSdk 36, Kotlin, no Compose.

Guidance for AI agents and human contributors working in this repo. Kept
machine-agnostic on purpose: it should hold for any fork/checkout, not one
person's laptop.

## Build & toolchain

- Build the debug APK from the repo root:
  ```
  ./gradlew :app:assembleDebug
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk`.
- **JDK is auto-provisioned.** The build declares a Java toolchain
  (`kotlin { jvmToolchain(17) }` in `app/build.gradle.kts`) and applies the
  Foojay resolver in `settings.gradle.kts`, so Gradle downloads/selects JDK 17
  for compilation regardless of the machine's default JDK. There is no need to
  set `JAVA_HOME` or prefix Gradle commands. Gradle itself (9.6.x) runs on any
  JDK from 17 to 26.
- **Android SDK is required** and is the one thing not auto-provisioned. Point
  the build at an SDK either via `local.properties` (`sdk.dir=/path/to/sdk`,
  gitignored) or the `ANDROID_HOME` environment variable. You need
  `platforms;android-36` and `build-tools;36.0.0` (AGP 9.3's defaults). See
  `CONTRIBUTING.md` for a from-scratch setup.
- Versions are centralized in `gradle/libs.versions.toml` (AGP + AndroidX /
  Material dependencies). AGP 9 ships built-in Kotlin, so the
  `org.jetbrains.kotlin.android` plugin is intentionally not applied.

## Devices

- `adb` is expected on `PATH` (e.g. Homebrew `android-platform-tools`). Don't
  hardcode absolute `adb`/SDK paths — they differ per machine.
- Prefer an emulator for exploratory or repro-driven UI testing. This is a
  portrait, D-pad/soft-key launcher, so any small AVD works; boot it and poll
  `adb shell getprop sys.boot_completed` until it returns `1`.
- A real physical device may be connected and **actively in use by the owner**.
  Don't drive it with synthetic `input keyevent`/`input tap` unless explicitly
  asked to test on hardware — it can collide with real input and navigate the
  user away from what they were doing.
- Some phones ship key-remapping apps (e.g. "Button Mapper") that intercept
  hardware keys, making synthetic `adb shell input keyevent` behave
  inconsistently (dropped/double-fired events). If hardware-key repro looks
  contradictory or flaky, switch to the emulator for clean signal.
- `uiautomator dump` can capture a transitional frame for fast-changing focus
  state (stale/non-monotonic). Prefer `exec-out screencap -p` with ~0.5-0.8s
  settle time after the triggering input, or cross-check
  `dumpsys window | grep mCurrentFocus` for the foregrounded Activity.

### Soft-key hardware mapping

The target device family (TCL T440W; input device `soc:matrix_keypad0`) never
emits Android `KEYCODE_SOFT_LEFT`/`KEYCODE_SOFT_RIGHT` (1/2) — nor MENU or
BACK, despite what the kernel event names suggest. Its key layout is
`/odm/usr/keylayout/soc_matrix_keypad0.kl` (ODM partition, root-only — not
readable over adb), and it maps the soft keys to the function-key codes:

| Physical key   | Kernel event        | Android code the app receives |
|----------------|---------------------|-------------------------------|
| Left soft key  | `KEY_MENU` (scan 22)| **131 `KEYCODE_F1`**          |
| Right soft key | `KEY_BACK` (scan 20)| **132 `KEYCODE_F2`**          |
| OK (center)    | `KEY_OK`            | 23 `DPAD_CENTER`              |
| Call key       | `KEY_SEND`          | 5 `KEYCODE_CALL`              |

So screens must treat **F1 as the left soft key** and **F2 as the right soft
key** (plus BACK where a right action is defined, for devices/emulators that
emit it). `util/SoftKeys.kt` defines the canonical code sets
(`LEFT_SOFT_KEYS`, `RIGHT_SOFT_KEYS`); SOFT_LEFT/SOFT_RIGHT are kept as
aliases. Careful: `KEYCODE_BREAK` is 121 and `KEYCODE_NUM` is 78 — do NOT
guess key-code values from memory; check them against the SDK
(`javap -constants` on `android.view.KeyEvent` in the platform jar).

To re-verify on a connected phone, never inject keys. Run both monitors, then
have the owner press one key at a time (~2 s apart) and correlate timestamps:

```
adb shell getevent -lt /dev/input/eventN   # matrix keypad; find N via getevent -pl
adb logcat                                  # correlate with the owner's presses
```
(there are no key-logging hooks in the repo; temporarily add a `dispatchKeyEvent`
`Log.d("FlipKeys", ...)` override to the Activities if app-side visibility is needed.)

## Project structure

`app/src/main/java/com/flipos/launcher/` is split into sibling packages:

- `activities/` — every screen Activity plus `BaseListActivity`.
- `ui/` — RecyclerView adapters and custom views.
- `data/` — prefs, repositories, and the built-in icon/wallpaper registries.
- `service/` — the notification listener.
- `util/` — small extensions.

Key pieces:

- `BaseListActivity` is shared scaffolding (title bar + RecyclerView +
  SoftKeyBar) for every vertical list screen (Options, Launcher Settings,
  Hide Apps, Shortcuts, App/Activity Picker, Notices). It also owns the
  accent-color theme-overlay-on-`onCreate` + recreate-on-resume-if-changed
  pattern — new list screens should extend it rather than reinventing this.
- `ListRowAdapter` + `Row` is the generic one-line-or-icon-row adapter reused
  across those list screens; `AppGridAdapter` is the App Drawer's icon grid;
  `NoticeRowAdapter` is the richer 3-line notice row.
- `LauncherPrefs` is the single SharedPreferences wrapper — all settings
  (icon size/shape/pack, accent color, drawer view mode, badges, shortcuts,
  hidden apps, per-app icon overrides) live there.
- `IconShapeRenderer` masks app icons into the user's chosen shape (adaptive
  icons composite their own fg/bg layers then get clipped; legacy icons get
  an optional synthesized tinted background disc).

## Bundled icons & wallpapers

Built-in icons and wallpapers live directly in
`app/src/main/res/drawable-nodpi/` — that folder is the single source of truth
(there are no separate staging folders). To add one:

1. Drop the PNG (icon) or JPG (wallpaper) into
   `app/src/main/res/drawable-nodpi/`.
2. Add the filename (no extension) to the matching registry:
   `BuiltInIcons.NAMES` in `data/BuiltInIcons.kt` or `BuiltInWallpapers.NAMES`
   in `data/BuiltInWallpapers.kt`.

Resources aren't auto-discovered, so step 2 is required for the asset to show
up in the picker.
