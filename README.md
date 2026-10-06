# VisionFit — Compose Multiplatform

The 8 screens of the *VisionFit App UI* design, implemented once in `composeApp/src/commonMain`
for Android, iOS and desktop (desktop is for quick previews on Windows).

| # | Screen | Package |
|---|--------|---------|
| 01 | Đăng nhập / Đăng ký | `presentation/auth` |
| 02 | Onboarding – chỉ số cơ thể | `presentation/onboarding` |
| 03 | Mục tiêu dinh dưỡng | `presentation/goal` |
| 04 | Dashboard hôm nay | `presentation/dashboard` |
| 05 | Chụp ảnh bữa ăn | `presentation/camera` |
| 06 | Đang phân tích (async) | `presentation/analysis` |
| 07 | Review & chỉnh sửa | `presentation/review` |
| 08 | Nhật ký bữa ăn | `presentation/history` |

## Modules

```
composeApp/   KMP library: all shared code and resources (android + iosArm64/iosSimulatorArm64 + jvm)
androidApp/   Android shell (MainActivity)
iosApp/       Xcode shell; builds the ComposeApp framework via :composeApp:embedAndSignAppleFrameworkForXcode
desktopApp/   JVM window sized like a phone (390 × 844)
```

## Architecture (commonMain)

```
core/          MviViewModel (state + one-shot effects), CollectEffects, TimeProvider, VnFormat
domain/        Pure Kotlin: model/, repository/ (interfaces), usecase/ (TDEE, day summary, streak, validation)
data/          mock/ (in-memory store + seed data + mock AI), repository/ (Mock* implementations)
di/            AppContainer — manual DI; swap Mock* for Ktor-backed repositories here
presentation/
  designsystem/  theme tokens (Color, Type, Dimens), components (BrutalSurface, buttons, pills, rings,
                 bottom bar / side rail, snackbar, motion), layout (window size classes, adaptive
                 helpers), icons (SVG paths from the design)
  <feature>/     <Feature>Contract.kt (UiState, Event, Effect), <Feature>ViewModel.kt,
                 <Feature>Screen.kt (stateful Route + stateless Screen + @Previews), components/
  navigation/    type-safe destinations + NavHost
  preview/       deterministic fixtures for previews and screenshot tests
```

Unidirectional flow per screen: `Screen(state, onEvent)` → `ViewModel.onEvent` → repositories/use cases
→ `state: StateFlow<UiState>`; navigation and messages go through `effects: Flow<Effect>`.

## Responsive layout

`designsystem/layout/WindowLayout.kt` measures the window once at the root (Material 3 size classes:
600 / 840 dp wide, 480 dp tall) and every screen picks its layout from `LocalWindowLayout`:

| Window | Navigation | Screens |
|--------|------------|---------|
| Phone, portrait (< 600 dp wide) | floating bottom bar | one column, as designed; below 360 dp gutters tighten and crowded rows stack |
| Phone, landscape (< 480 dp tall) | side rail | two panes (photo · details, summary · list); footers shrink to one row |
| Tablet, portrait (600–839 dp) | side rail | one centered column, at most 640 dp wide |
| Tablet landscape, desktop (≥ 840 dp) | side rail | two panes, at most 1180 dp wide |

Inside screens, components adapt to the space they get: `AdaptiveGrid` picks its column count from the
available width, `ScaleDownToFit` shrinks fixed compositions (the sign-in collage), `proportionalHeight`
keeps photos from turning into strips, and `withFontScale()` makes text-driven breakpoints grow with the
system font size. `ResponsiveScreenshotTest` renders every screen at 320 × 640, 844 × 390, 390 × 844 with
130 % text, 834 × 1194, 1194 × 834 and 1280 × 800 into `composeApp/build/screenshots/responsive/`.

## Run

```bash
./gradlew :androidApp:installDebug     # Android (compileSdk 37)
./gradlew :desktopApp:run              # desktop preview
./gradlew :composeApp:jvmTest          # unit, flow, screenshot and end-to-end tests
```

iOS: on macOS open `iosApp/iosApp.xcodeproj` and run (set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` for devices).

Screenshot tests write every screen and edge state to `composeApp/build/screenshots/`.

## Demo data

* Login: `an@visionfit.vn` / `visionfit123` (`locked@visionfit.vn` shows the locked-account error). "Đăng ký" creates a new account and goes through onboarding.
* "Today" is the real date. The seed matches the design: 1.663 / 2.034 kcal eaten, a dinner tray waiting for AI, a 6-day streak with two days over target. Yesterday also has a failed analysis and a very long dish name.
* Opening the dinner card runs the mock AI: recognizing (3.5 s) → calculating (3.5 s) → done, 4 dishes, 819 kcal.
* New photos: every second one fails on its first attempt so the error and retry states can be seen. "Tự nhập món ăn" opens an empty review.
* The calendar button in the diary opens a date picker; older days are empty.
* `MockNetworkMonitor.setOnline(false)` shows the offline banner and the "waiting for network" card (also in `@Preview`s).

## Notes

* The camera is behind `CameraSource` (domain). The mock returns bundled photos; a real build implements it with CameraX/PhotoPicker and AVFoundation/PHPicker.
* Photos use `bundled://` URIs resolved in `MealPhotoImage`; remote URLs would go through an image loader such as Coil.
* UI copy is Vietnamese and inline. Move it to `composeResources/values/strings.xml` to localize.
* Fonts: Baloo 2 and Be Vietnam Pro, SIL Open Font License (see `licenses/`).
