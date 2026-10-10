# VisionFit KMP: Nhật ký học UI (Clean Architecture + Compose Multiplatform)

> File này ghi lại một chuỗi buổi học giữa user và Claude.
> Buổi 1: bước 1 → 5. Buổi 2 (2026-10-10): dạy lại bước 5, làm rõ DI, bước 6.1 → 6.3.
> Dán file này vào một cuộc chat mới để Claude tiếp tục dạy đúng chỗ đã dừng.

---

## 0. Dành cho Claude: đọc kỹ trước khi trả lời

**Vai trò:** Bạn là mentor 1-1, dạy user cách xây dựng tầng UI của một dự án Kotlin Multiplatform + Compose theo Clean Architecture, lấy dự án `visionfit-kmp` làm ví dụ.

**Về user:**
- Người Việt. **Luôn trả lời bằng tiếng Việt.**
- Đang tự học. Đã làm Spring Boot backend khá tốt, nhưng mới với Compose, MVI và kiến trúc UI.
- Muốn **tự gõ code**. Claude chỉ đưa snippet minh họa nhỏ, giao bài tập, rồi review nghiêm khắc. Không viết hộ cả màn hình.

**Phong cách dạy bắt buộc** (rút ra từ chính buổi học này):
1. **Đi từ nền lên, theo đúng lộ trình ở mục 2. Không được nhảy cóc.** Ở đầu buổi, Claude giải thích ngay bằng feature Auth trong khi user chưa biết setup, theme và MVI. User đã rất bực và phải yêu cầu học lại từ phần setup.
2. **Mỗi lượt chỉ dạy một bước.** Cuối mỗi lượt có checkpoint ("đã rõ chưa?") và một bài tập nhỏ.
3. Với mỗi file, phải nói đủ 3 điều:
   - **Vì sao file này tồn tại** (nó giải quyết vấn đề gì)
   - **Vì sao nó được viết sau file nào** (thứ tự phụ thuộc: file A `import` file B thì B phải có trước)
   - **Từng khối code để làm gì**
4. **Giới thiệu khái niệm trước khi dùng.** Ví dụ user đã hỏi "MVI là gì?". Hãy định nghĩa trước rồi mới dùng thuật ngữ.
5. Dùng các ẩn dụ đã quen với user:
   - CompositionLocal = **phát sóng**
   - StateFlow = **cái hộp**
   - Channel = **đường ống**
   - Back stack = **chồng đĩa**
   - Domain / Data / DI = **thực đơn / nhà bếp / quản lý**
6. Mở đầu bằng **vấn đề** (cách viết "ngây thơ" sẽ hỏng ở đâu), sau đó mới đưa ra **giải pháp**.
7. Khi user nói "lấy ví dụ", hãy lấy **code thật trong dự án**, ưu tiên phần Auth, và dẫn `file:dòng`. Chỉ dùng phần Auth sau khi khái niệm nền đã được dạy.
8. Dùng bảng và sơ đồ ASCII khi cần, nhưng tránh dài dòng.
9. Nếu có quyền đọc repo, **hãy kiểm tra lại file và số dòng trước khi trích dẫn**, vì code có thể đã thay đổi sau buổi học này.
10. Thứ tự file trong mục 3 là thứ tự **phụ thuộc logic**. Git không có lịch sử từng file, vì cả dự án được commit một lần.

---

## 1. Bối cảnh dự án

- **Repo:** GitHub `Diendien1801/BaoFit`. User học trên nhiều máy:
  - Windows: `C:\Users\dient\IdeaProjects\microservicePractice\visionfit-kmp`
  - Mac: `/Users/admin/Dien/backend/BaoFit`
  - Đầu buổi nên `git pull` và kiểm tra lại trạng thái repo.
- **Ứng dụng:** VisionFit, app chụp ảnh bữa ăn để AI ước lượng dinh dưỡng. Giao diện tiếng Việt.
- **Công nghệ:**
  - Kotlin 2.4.20, Compose Multiplatform 1.12.1, Material3 1.9.0
  - navigation-compose 2.9.2, lifecycle 2.11.0
  - Gradle 9.7.1, JDK 21
- **Nền tảng đích:** Android, iOS, Desktop (JVM)
- **Dữ liệu:**
  - **Auth đã nối backend thật** (commit `607f553`): `RemoteAuthRepository` dùng Ktor gọi `identity-service`, lưu token bằng `TokenStore`.
  - Các repository còn lại vẫn là **Mock**.
  - Backend thật là project Spring Boot microservices riêng (Windows: `C:\Users\dient\IdeaProjects\visionfit-backend\`), do user tự xây, `identity-service` phụ trách auth.
  - File `domain/repository/Repositories.kt` chính là hợp đồng mà backend phải đáp ứng.
- **Chọn bếp trong `AppContainer`** (`di/AppContainer.kt:44-45`): `if (apiBaseUrl == null) MockAuthRepository(store) else RemoteAuthRepository(...)`.
  - `AppGraph` (App.kt:66) truyền `apiBaseUrl = defaultApiBaseUrl`. Đây là `expect/actual` theo nền tảng: Android `http://10.0.2.2:8081/`, JVM và iOS `http://localhost:8081/`. Vì vậy app thật mặc định dùng bếp thật.
  - Test và preview gọi `AppContainer()` không có URL, nên dùng bếp giả.
- **Cấu trúc package** (`composeApp/src/commonMain/kotlin/com/visionfit/`):
  ```
  App.kt         điểm vào chung cho mọi nền tảng
  core/          công cụ dùng chung (mvi/, time/, format/)
  domain/        model, interface repository, use case (Kotlin thuần)
  data/          mock store + Mock*Repository
  di/            AppContainer
  presentation/  designsystem/, navigation/, common/, preview/, auth/, onboarding/,
                 goal/, dashboard/, history/, camera/, analysis/, review/
  ```

**Trạng thái màn Auth (cập nhật buổi 2, ngày 2026-10-10):**
- Bản gốc **đầy đủ** đã được khôi phục, không còn bị comment:
  - `presentation/auth/AuthScreen.kt` (448 dòng): `AuthRoute` ở dòng 87-106, `AuthScreen` ở dòng 109
  - `auth/components/AuthForm.kt`, `auth/components/AuthHero.kt`
- `AuthPracticeScreen.kt` và `myBlankScreen()` **không còn**.
- **Cách luyện tập đã thống nhất** (bài tập 3 của bước 6.3):
  - User tạo `presentation/auth/practice/MyAuthScreen.kt` với chữ ký `MyAuthScreen(state: AuthUiState, onEvent: (AuthEvent) -> Unit, modifier: Modifier = Modifier)`.
  - Đổi **đúng 1 dòng** `AuthScreen(...)` → `MyAuthScreen(...)` ở dòng 105 trong `AuthRoute`.
  - Bản gốc `AuthScreen` giữ nguyên để đối chiếu. Học xong thì trả dòng 105 lại như cũ.
  - Nếu user chưa làm bước này, hãy nhắc làm trước khi dạy 6.4.

---

## 2. Lộ trình học

| # | Bước | Trạng thái |
|---|---|---|
| 1 | Setup dự án (Gradle, các module, source set, App "Hello") | ✅ Đã dạy |
| 2 | Theme (Color, Type, Dimens, Theme) | ✅ Đã dạy. User nói "đã thấm" |
| 3 | Khung MVI (MviViewModel, CollectEffects) + "MVI là gì" | ✅ Đã dạy |
| 4 | Navigation (Routes, NavHost) + "ai kích hoạt navigate" (chuỗi 4 trạm, ví dụ Camera và Auth) | ✅ Đã dạy. User nói "hiểu cấu hình nav rồi" |
| 5 | Domain → Data → DI | ✅ Đã dạy lại ở buổi 2. User hỏi thêm về DI và CompositionLocal, đã giải thích (xem 3.5b) |
| 6.1 | `AuthContract` | ✅ Đã dạy |
| 6.2 | `AuthViewModel` | ✅ Đã dạy. User nói "hiểu rồi" |
| 6.3 | `AuthRoute` (tách Route và Screen) | ✅ Đã dạy. **Chưa xác nhận** checkpoint |
| 6.4 | **User tự viết `MyAuthScreen`** | ⏭ **TIẾP THEO** |
| 7 | Design system components (BrutalSurface, VfPrimaryButton, icon, thứ tự Modifier) | Chưa |
| 8 | Hoàn thiện App shell (AppMessenger/Snackbar, WindowLayout responsive, insets, VisionFitPreview) | Chưa |
| 9 | Test (FlowTests cho ViewModel, UI test, screenshot test) | Chưa |
| 10 | Thay Mock bằng backend thật (Ktor, RemoteAuthRepository) | **Code đã có** (commit `607f553`). Khi tới bước này, hãy dạy bằng cách đọc và review code có sẵn, không viết lại |

---

## 3. Nội dung đã dạy (đủ chi tiết để trình bày lại)

### Bước 1: Setup dự án

**Cách tạo:** dùng wizard Kotlin Multiplatform (Android Studio hoặc kmp.jetbrains.com). Wizard sinh sẵn khung Gradle, sau đó dự án được chỉnh thêm.

**Các module:**
- `composeApp`: module chính, chứa **toàn bộ code dùng chung** (UI và logic)
- `androidApp`, `desktopApp`, `iosApp`: các vỏ mỏng, chỉ gọi `VisionFitApp()`
- `androidApp` phải là module riêng vì Android cần một module kiểu "application" để sinh APK. `composeApp` dùng plugin `com.android.kotlin.multiplatform.library` nên chỉ là thư viện.

**Các file Gradle, theo thứ tự đọc:**

1. `gradle/wrapper/gradle-wrapper.properties`
   - Ghim Gradle 9.7.1, để ai clone dự án về cũng dùng đúng phiên bản này.
2. `gradle/gradle-daemon-jvm.properties`
   - `toolchainVersion=21`: Gradle chạy bằng JDK 21.
3. `gradle.properties`
   - `-Xmx4096M`: cho Gradle 4GB RAM.
   - `kotlin.native.ignoreDisabledTargets=true`: đang ở Windows thì bỏ qua iOS mà không báo lỗi.
   - Bật caching và configuration-cache để build lần sau nhanh hơn.
   - `android.useAndroidX=true`.
4. `settings.gradle.kts`
   - Tên dự án.
   - `TYPESAFE_PROJECT_ACCESSORS`: cho phép viết `projects.composeApp`.
   - Khai báo kho tải plugin và thư viện (google, mavenCentral, gradlePluginPortal).
   - `include` 3 module. iOS không có ở đây vì nó là project Xcode.
5. `gradle/libs.versions.toml` (version catalog)
   - Ba khu: `[versions]`, `[libraries]`, `[plugins]`.
   - Mục đích: khai báo phiên bản ở một chỗ, trong file build chỉ cần viết `libs.xxx`.
6. `build.gradle.kts` (gốc)
   - Khai báo các plugin với `apply false`: khai báo nhưng không bật ở gốc, để mọi module dùng chung một phiên bản.
7. `composeApp/build.gradle.kts`
   - **Plugin:**
     - `kotlinMultiplatform`
     - `androidMultiplatformLibrary`
     - `composeMultiplatform`
     - `composeCompiler`
     - `kotlinxSerialization` (dùng cho route của navigation)
   - **Target:** `android {}`, `iosArm64()` + `iosSimulatorArm64()` (đóng gói thành framework `ComposeApp`), `jvm()` (desktop, đồng thời để test và preview nhanh).
   - **Source set:**
     - `commonMain`: compose runtime/foundation/ui/material3, components-resources, ui-tooling-preview, lifecycle-viewmodel-compose, lifecycle-runtime-compose, navigation-compose, coroutines-core, datetime, serialization-core
     - `commonTest`: kotlin-test, coroutines-test
     - `androidMain`: activity-compose
     - `jvmMain`: coroutines-swing
     - `jvmTest`: compose.desktop.currentOs, compose ui-test
   - `compose.resources { packageOfResClass = "com.visionfit.resources" }`: vị trí của class `Res` tự sinh, dùng để đọc font và ảnh trong `composeResources/`.
   - `androidRuntimeClasspath(libs.compose.uiTooling)`: để xem preview trên Android.
8. `androidApp/build.gradle.kts`
   - `implementation(projects.composeApp)`, `applicationId`, `minSdk`/`targetSdk`.
9. `desktopApp/build.gradle.kts`
   - `compose.desktop.application { mainClass = "com.visionfit.desktop.MainKt" }`.
10. `iosApp/`
    - Project Xcode, gọi `MainViewControllerKt.MainViewController()`.

**Các source set trong `composeApp/src`:**
- `commonMain/kotlin`: gần như toàn bộ code
- `commonMain/composeResources`: `font/`, `drawable/`
- `iosMain`
- `commonTest`
- `jvmTest`

**App đầu tiên:**
```kotlin
@Composable fun VisionFitApp() { Text("Hello") }
```
Ba điểm vào gọi hàm này:
- Android: `MainActivity` → `enableEdgeToEdge()` rồi `setContent { VisionFitApp() }`
- Desktop: `main.kt` → `application { Window(size 390x844) { VisionFitApp() } }`
- iOS: `MainViewController() = ComposeUIViewController { VisionFitApp() }`

Chạy bản desktop: `./gradlew :desktopApp:run`.

**Các package rỗng tạo sẵn:** `core/`, `domain/`, `data/`, `di/`, `presentation/`.

---

### Bước 2: Theme (`presentation/designsystem/theme/`)

**Mục tiêu:** thay `Color(0xFF5B3FE0)` và `fontSize = 30.sp` bằng `VisionFitTheme.colors.primary` và `VisionFitTheme.type.headlineL`. Khi đó đổi màu cho cả app chỉ cần sửa 1 dòng.

**Thứ tự viết:** `Color.kt`, `Type.kt`, `Dimens.kt` (độc lập nhau) → `Theme.kt` → sửa `App.kt`.

**Khái niệm CompositionLocal ("phát sóng" thay cho việc truyền tham số qua nhiều tầng):**
```kotlin
val LocalXxx = staticCompositionLocalOf { mặcĐịnh }                    // 1. tạo kênh
CompositionLocalProvider(LocalXxx provides giáTrị) { ... }              // 2. phát
LocalXxx.current                                                        // 3. bắt sóng
```

**Color.kt**
- `@Immutable data class VisionFitColors(...)`, mọi màu đều có giá trị mặc định.
- `@Immutable` giúp Compose bớt vẽ lại.
- Quy ước tên màu, ví dụ với màu vàng: `yellow` (đậm), `yellowContainer` (nền nhạt), `onYellow` (màu chữ đặt trên nền vàng).
- `ink` là màu dùng cho mọi viền, chữ và bóng.
- Có các màu suy ra từ màu khác: `scrim`, `dishMarkers`.
- `internal val LocalVisionFitColors = staticCompositionLocalOf { VisionFitColors() }`.

**Type.kt**
- `@Immutable data class VisionFitTypography(headlineL, button, body, input, label, ...)`, mỗi field là một `TextStyle`, không có giá trị mặc định.
- File font đặt trong `composeResources/font/`. Plugin tự sinh `Res.font.xxx`.
- `balooFamily()` (cho tiêu đề, số, nút) và `beVietnamFamily()` (cho đoạn văn, hiển thị dấu tiếng Việt đẹp) là `@Composable`, vì `Font(Res.font...)` chỉ gọi được trong composable.
- `buildTypography` dùng hai hàm phụ `baloo()` và `body()` để khỏi lặp code.
- `lineHeight` tính bằng `em`, tức là bội số của cỡ chữ.
- `TightLineHeight` và giá trị `1.02` là mẹo sửa lỗi hiển thị font trên iOS và desktop. Có thể bỏ qua.
- `rememberVisionFitTypography()` dùng `remember` để chỉ tạo một lần.
- `LocalVisionFitTypography` có giá trị mặc định là `error(...)`, nên quên bọc theme thì app crash kèm thông báo rõ ràng.

**Dimens.kt**
- `object VfDimens`: Border 2dp, ShadowXs đến ShadowXL, InputHeight 54, CtaHeight 58, …
- `object VfRadius`: các mức bo góc.
- Dùng `object` và không cần kênh phát sóng, vì kích thước không bao giờ đổi theo theme.

**Theme.kt**
- `object VisionFitTheme { val colors @Composable @ReadOnlyComposable get() = LocalVisionFitColors.current; val type ... }` là lối tắt để đọc màu và chữ.
- `@Composable fun VisionFitTheme(content)` trùng tên với object. Kotlin cho phép điều này, `MaterialTheme` cũng làm như vậy. Hàm này:
  - `remember` bảng màu, gọi `rememberVisionFitTypography()`
  - Dịch sang Material 3 bằng `lightColorScheme(...)` và `Typography(...)`, để các component có sẵn như Snackbar hay vòng xoay loading cũng đúng màu thương hiệu
  - Gọi `CompositionLocalProvider(LocalVisionFitColors/Typography provides ...)`
  - Gọi `MaterialTheme(...)`
  - Đặt `LocalContentColor provides colors.ink` để màu chữ mặc định là ink
- `App.kt` lúc này: `VisionFitTheme { Text("Hello", color = VisionFitTheme.colors.primary, style = VisionFitTheme.type.headlineL) }`.

**Câu hỏi của user:** "Bọc preview bằng `VisionFitTheme { }` để làm gì?"

Trả lời: để bên trong dùng được `.colors` và `.type`. App thật được bọc sẵn trong `App.kt`, còn `@Preview` chỉ vẽ đúng hàm được chỉ định, không đi qua `App.kt`. Nếu quên bọc:
- `type` → crash
- `colors` → không crash (vì có mặc định), nên dễ tưởng nhầm là không cần bọc
- `MaterialTheme.colorScheme` → ra màu mặc định của Google

Dự án có sẵn hàm bọc `presentation/preview/VisionFitPreview.kt`, gồm theme + `ProvideWindowLayout`.

**Bài tập đã giao:**
1. Thay màu viết thẳng trong `myBlankScreen` bằng màu của theme.
2. Bọc preview bằng theme.
3. Thử đổi giá trị `primary`.

---

### Bước 3: Khung MVI (`core/mvi/`)

**Vấn đề:** viết `var count by remember { mutableStateOf(0) }` ngay trong màn hình có 3 nhược điểm:
- xoay máy là mất dữ liệu
- logic nằm lẫn trong UI
- không test được nếu không bật giao diện

**4 khái niệm:**
- **ViewModel:** sống sót khi xoay máy, bị hủy khi đóng màn hình
- **Coroutine:** `launch` và `suspend`; `viewModelScope` tự hủy theo ViewModel
- **StateFlow** = cái hộp: giữ giá trị hiện tại, báo cho người đang theo dõi khi giá trị đổi
- **Channel** = đường ống: mỗi món chỉ được lấy ra đúng 1 lần

**MVI có 3 loại dữ liệu:**
- **State:** ảnh chụp toàn bộ màn hình, nằm trong hộp
- **Event:** việc người dùng vừa làm, đi vào qua `onEvent`
- **Effect:** việc chỉ xảy ra 1 lần (chuyển màn, hiện thông báo), đi qua ống

Effect không được để trong State, vì xoay máy thì UI đọc lại hộp và sẽ chuyển màn thêm lần nữa.

**MviViewModel.kt**
```kotlin
abstract class MviViewModel<State, Event, Effect>(initialState: State) : ViewModel() {
    private val _state = MutableStateFlow(initialState); val state: StateFlow<State> = _state.asStateFlow()
    private val _effects = Channel<Effect>(Channel.BUFFERED); val effects: Flow<Effect> = _effects.receiveAsFlow()
    protected val currentState: State get() = _state.value
    abstract fun onEvent(event: Event)
    protected fun updateState(reducer: State.() -> State) { _state.update(reducer) }
    protected fun sendEffect(effect: Effect) { viewModelScope.launch { _effects.send(effect) } }
}
```
- Hộp và ống đều có 2 lớp: `_x` sửa được (private), `x` chỉ đọc (public).
- `BUFFERED`: effect gửi lúc chưa ai nghe sẽ nằm chờ trong ống.
- `State.() -> State` là lambda "đứng bên trong" State, nên viết gọn được `updateState { copy(count = count + 1) }`.
- `copy` tạo object mới. Compose thấy object mới thì vẽ lại.
- `send` là `suspend`, nên phải bọc trong `launch`.

**CollectEffects.kt** (người đứng ở đầu kia của ống, phía UI)
- `LocalLifecycleOwner.current`: biết màn hình đang hiện hay đang ẩn.
- `rememberUpdatedState(onEffect)`: luôn gọi callback mới nhất mà không phải mở lại việc nghe ống.
- `LaunchedEffect(effects, lifecycleOwner)`: coroutine gắn với composable.
- `repeatOnLifecycle(STARTED)`: chỉ nghe khi màn hình đang hiện. App vào nền thì effect nằm chờ.
- `withContext(Dispatchers.Main.immediate) { effects.collect { latestOnEffect(it) } }`: xử lý ngay, không để lỡ effect.

**Ví dụ dùng (đếm số):**
- Có `CounterState`, `CounterEvent.Plus`, `CounterEffect.ReachedTen` và `CounterViewModel`.
- UI:
  - `viewModel { CounterViewModel() }`: tạo lần đầu, các lần sau trả lại đúng ViewModel cũ
  - `collectAsStateWithLifecycle()`: đọc hộp
  - `CollectEffects`: nghe ống
  - `onEvent(...)`: gửi event

**Câu hỏi của user:** "MVI là gì, có phải một loại kiến trúc?"

Trả lời:
- MVI = Model–View–Intent, là **pattern cho riêng tầng presentation**. Clean Architecture là cách chia **cả app** thành các tầng. MVI nằm bên trong tầng presentation.
- Trong dự án: Model = `State`, View = `Screen`, Intent = `Event`. Intent được đổi tên thành Event để không nhầm với `Intent` của Android.
- So với MVVM: MVVM có nhiều state rời và nhiều hàm. MVI có 1 object State, 1 cửa vào `onEvent`, và dữ liệu chạy một chiều.
- Dự án này **lai**: dùng class `ViewModel` của Google nhưng tổ chức theo kiểu MVI.
- Ưu điểm: dễ debug, dễ test, không có trạng thái mâu thuẫn. Nhược điểm: nhiều class hơn.

**Bài tập đã giao:** tự gõ ví dụ đếm số, thêm nút "−" (không xuống dưới 0), nút "Reset", và Effect `ShowMessage(text)` khi Reset.

---

### Bước 4: Navigation (`presentation/navigation/`)

(DI được tách ra bước 5, vì DI chỉ có ý nghĩa khi đã có repository.)

**Khái niệm:**
- **Back stack** = chồng đĩa
- **NavController** = người quản lý chồng đĩa, với `navigate(X)` (thêm đĩa) và `popBackStack()` (nhấc đĩa)
- **NavHost** = tấm bản đồ "tên màn hình → hàm vẽ", luôn hiển thị đĩa trên cùng

**Routes.kt**
- Mỗi màn hình có một tên dạng `@Serializable data object` (không có tham số) hoặc `data class` (có tham số), ví dụ `AnalysisDestination(val jobId: String)`, `ReviewDestination(jobId, manualEntry = false)`.
- Cách này an toàn kiểu dữ liệu: gõ sai hay thiếu tham số thì compiler bắt ngay, thay vì ghép chuỗi `"review/$id"` rồi crash lúc chạy.
- `@Serializable` cần thiết để thư viện lưu chồng đĩa. Đây là lý do có plugin serialization từ bước setup.

**VisionFitNavHost.kt**
- `navController = rememberNavController()` là tham số có giá trị mặc định, để test truyền controller khác vào.
- `NavHost(startDestination = AuthDestination, ...)` kèm 4 hiệu ứng fade.
- `composable<X> { XRoute(onNavigateToY = { navController.navigate(Y) }) }`.
- **Màn hình chỉ nhận lambda, không bao giờ nhận `navController`.** Nhờ vậy:
  - màn hình không phụ thuộc thư viện navigation
  - preview và test dễ
  - toàn bộ luồng đi của app nằm trong một file
- `entry.toRoute<X>()` để lấy tham số ra khỏi đĩa.

**Các tùy chọn khi navigate:**
- `launchSingleTop`: không chồng thêm X nếu X đã ở trên cùng (chống bấm 2 lần)
- `popUpTo<X> { inclusive = true }`: nhấc bỏ đĩa cho tới X, tính cả X. Ví dụ Camera → Analysis thì bỏ Camera.
- `popUpTo(graph.id) { inclusive = true }`: xóa sạch chồng đĩa (sau khi đăng nhập)

**Hai hàm phụ (extension function):**
- `navigateToDashboard()`: nếu đã có Dashboard bên dưới thì pop về đó; nếu chưa thì xóa sạch rồi đặt Dashboard.
- `navigateToTab()`: kiểu thanh tab dưới đáy, dùng `popUpTo<Dashboard>{ saveState = true }`, `launchSingleTop`, `restoreState`.

**App.kt lúc này:** `VisionFitTheme { VisionFitNavHost() }`.

**Câu hỏi của user:** "Chỗ nào kích hoạt navigate? Có liên quan Domain, Data, DI không?"

Trả lời: **không liên quan.** Điều hướng nằm hoàn toàn trong tầng presentation, theo một chuỗi tiếp sức 4 trạm:
```
① Screen    onEvent(Event)                 "người dùng làm gì"
② ViewModel sendEffect(NavigateXxx)        "có nên đi không, đi đâu, mang theo gì"
③ Route     CollectEffects → onNavigateXxx()  "effect này ứng với lambda nào"
④ NavHost   navController.navigate(...)    "đi bằng cách nào" (chỉ ở đây mới có navController)
```
- **Ví dụ Camera, nút Close:**
  - `CameraScreen` gọi `onEvent(CameraEvent.Close)`
  - `CameraViewModel` gọi `sendEffect(NavigateBack)`
  - `CameraRoute` gọi `onNavigateBack()`
  - NavHost chạy `popBackStack()`
- **Ví dụ Camera, nút chụp:**
  - `submit()` gọi `analysisRepository.submitPhoto` và nhận về `jobId`
  - Thành công thì `NavigateToAnalysis(jobId)`, thất bại thì `SubmitFailed` (chỉ hiện thông báo)
  - NavHost chạy `navigate(AnalysisDestination(jobId)) { popUpTo<Camera>{inclusive=true} }`
- **Ví dụ Auth** (user yêu cầu), cùng event `Submit` có 4 kết cục:
  - email sai định dạng → lỗi đỏ dưới ô, không đi
  - sai mật khẩu hoặc tài khoản bị khóa → banner `submitError`, không đi
  - đăng ký thành công → `NavigateToOnboarding`
  - đăng nhập thành công → `NavigateToDashboard`
- Ở NavHost:
  - Dashboard dùng `navigateToDashboard()`, xóa sạch chồng đĩa, nên bấm Back là thoát app
  - Onboarding dùng `navigate` bình thường, Auth vẫn nằm bên dưới
- Effect không chỉ để chuyển màn: `OpenUrl` mở trình duyệt, `ResetLinkSent`/`ResetLinkFailed` hiện thông báo.
- Data chỉ **cung cấp kết quả** (`AuthResult`, `jobId`) để ViewModel quyết định. Data không tự điều hướng.
- Lý do luôn đi qua ViewModel: mọi quyết định nằm một chỗ, test được, và mọi màn hình theo cùng một mẫu.

**Bài tập đã giao:**
- Dựng một NavHost mini với `HomeDestination` (object) và `DetailDestination(name)` (class), hai màn hình chỉ nhận lambda, kèm nút "Về Home và xóa lịch sử" dùng `popUpTo`.
- Sau đó thêm `HomeViewModel` với event `DishClicked(name)` và effect `OpenDetail(name)`, tách thành `HomeRoute`/`HomeScreen`, để luồng đi đủ 4 trạm.

---

### Bước 5: Domain → Data → DI

**Ẩn dụ nhà hàng:**
- Domain = **thực đơn** (gọi được món gì)
- Data = **nhà bếp** (món được nấu thế nào)
- DI = **quản lý** (phân công bếp nào phục vụ)
- ViewModel = **phục vụ**, chỉ cần đọc thực đơn

**Thứ tự viết:**
```
DOMAIN ① Auth.kt → ② Repositories.kt → ③ ValidateCredentialsUseCase.kt
DATA   ④ TimeProvider.kt → ⑤ InMemoryVisionFitStore.kt → ⑥ MockAuthRepository.kt
DI     ⑦ AppContainer.kt → ⑧ ViewModelFactory.kt → ⑨ sửa App.kt → ⑩ AuthRoute dùng containerViewModel
```

**Luật của Domain:** Kotlin thuần, không import `androidx` hay `compose`.

① **`domain/model/Auth.kt`**
- `data class UserSession(userId, email, displayName)`, có thuộc tính `initials` ("Trần Minh Thư" → "TT"). Đây là quy tắc dữ liệu nên đặt ở domain.
- `enum class AuthError { INVALID_CREDENTIALS, EMAIL_ALREADY_REGISTERED, ACCOUNT_LOCKED, NETWORK }`. Dùng enum chứ không dùng String, vì domain không biết tiếng Việt. UI tự dịch.
- `sealed interface AuthResult { Success(session), Failure(error) }`:
  - `when` buộc phải xử lý đủ cả 2 nhánh
  - thay cho việc throw exception, lỗi trở thành dữ liệu bình thường

② **`domain/repository/Repositories.kt`** (interface `AuthRepository`)
- `val session: StateFlow<UserSession?>`
- `suspend fun login(email, password): AuthResult`, `register(...)`, `requestPasswordReset(email): Boolean`, `logout()`
- Interface là lời hứa, không có thân hàm. Đây chính là **hợp đồng với `identity-service`**.

③ **`domain/usecase/ValidateCredentialsUseCase.kt`** (use case = quy tắc nghiệp vụ tách thành class riêng)
- `enum CredentialError { EMAIL_BLANK, EMAIL_INVALID, PASSWORD_BLANK, PASSWORD_TOO_SHORT, CONFIRMATION_MISMATCH }`: lỗi nhập liệu, kiểm tra ngay trên máy. Khác với `AuthError` là lỗi server: lỗi nhập liệu hiện dưới từng ô, lỗi server hiện thành banner.
- `data class CredentialValidation(email?, password?, confirmation?)` có `isValid`. Trả về lỗi của cả 3 ô cùng lúc.
- `operator fun invoke(email, password, confirmation: String? = null)`: gọi được như hàm. `confirmation = null` khi đăng nhập.
- `companion object { MIN_PASSWORD_LENGTH = 8; EMAIL_REGEX }`.
- Tách khỏi ViewModel để test riêng được và dùng lại được, ví dụ cho màn đổi mật khẩu.

④ **`core/time/TimeProvider.kt`**
- Không liên quan auth. Store ở ⑤ cần nó.
- Interface `TimeProvider` có 2 bản: `SystemTimeProvider` (giờ thật) và `FixedTimeProvider` (giờ cố định cho test).

⑤ **`data/mock/InMemoryVisionFitStore.kt`** (database giả)
- `internal data class MockAccount(userId, email, password, displayName, isLocked)`.
- `accounts = MutableStateFlow(listOf(an@visionfit.vn / visionfit123, locked@visionfit.vn bị khóa))`, đóng vai bảng `users`.
- Hằng số `DEMO_EMAIL`, `DEMO_PASSWORD`, `LOCKED_EMAIL`.
- Tách store riêng vì mọi Mock repository (auth, meal, profile…) dùng chung, để dữ liệu khớp nhau.

⑥ **`data/repository/MockAuthRepository.kt`**
- `internal class MockAuthRepository(store, latencyMillis = 900) : AuthRepository`. Độ trễ giả để thấy được trạng thái loading.
- `_session`/`session`: pattern bên trong sửa, bên ngoài chỉ đọc.
- `login`:
  - `delay` để giả mạng
  - tìm email không phân biệt hoa thường
  - `when`: sai email hoặc sai mật khẩu → `INVALID_CREDENTIALS`; tài khoản khóa → `ACCOUNT_LOCKED`; còn lại → `signIn`
  - **Mật khẩu được kiểm tra trước khi báo "bị khóa"**, để người đoán mò không biết email có tồn tại hay đang bị khóa. Backend thật cũng nên làm vậy.
- `register`:
  - chuẩn hóa email về chữ thường
  - trùng email → `EMAIL_ALREADY_REGISTERED`
  - `store.accounts.update { it + account }` rồi `signIn`
- `signIn` tạo `UserSession`, gán vào `_session`, trả về `Success`.
- `displayNameFromEmail` lấy tên từ email: "minh.tran@x.vn" → "Minh". Hàm này đã được tách ra `data/repository/DisplayNames.kt` để Mock và Remote dùng chung.
- Bếp thứ hai đã có: `RemoteAuthRepository(client, tokenStore) : AuthRepository` (`RemoteAuthRepository.kt:35`). **Một thực đơn, hai bếp**, nên các file khác giữ nguyên.

**DI là gì:** class không tự tạo những thứ nó cần, mà nhận chúng qua constructor.
- Cách đúng: `AuthViewModel(authRepository: AuthRepository, validateCredentials)`.
- Cách sai: tự tạo `MockAuthRepository()` bên trong ViewModel.

⑦ **`di/AppContainer.kt`**
- `class AppContainer(timeProvider = SystemTimeProvider(), applicationScope = ...)`
- Bên trong:
  - `private val store = InMemoryVisionFitStore(timeProvider)`
  - `val authRepository: AuthRepository = if (apiBaseUrl == null) MockAuthRepository(store) else RemoteAuthRepository(...)` (dòng 44-45) ← **chỗ duy nhất trong app viết tên bếp cụ thể**
  - `val validateCredentials = ValidateCredentialsUseCase()`
- Kiểu khai báo là interface, nên bên ngoài không biết bên trong là Mock.
- Tham số có giá trị mặc định để test truyền vào được. `applicationScope` dành cho việc phân tích ảnh chạy nền.
- `val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error(...) }` là kênh phát sóng.

⑧ **`presentation/common/ViewModelFactory.kt`**
```kotlin
@Composable inline fun <reified VM : ViewModel> containerViewModel(key: String? = null, crossinline create: AppContainer.() -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key) { container.create() }
}
```
- `viewModel {}` tạo ViewModel lần đầu, các lần sau trả lại cái cũ. ViewModel bị hủy khi màn hình bị pop khỏi chồng đĩa.
- `AppContainer.() -> VM` là lambda đứng bên trong container, nên viết thẳng được `authRepository`.
- `inline reified` để hàm biết được kiểu class lúc chạy. User chỉ cần biết cách dùng.

⑨ **`App.kt`**
- `VisionFitApp(container: AppContainer = AppGraph.container)` → `VisionFitTheme { CompositionLocalProvider(LocalAppContainer provides container) { VisionFitNavHost() } }`.
- `object AppGraph { val container by lazy { AppContainer() } }`: cả app chỉ có 1 container, và chỉ tạo khi lần đầu cần tới.

⑩ **`AuthRoute`** (trong `AuthPracticeScreen.kt`)
- `viewModel: AuthViewModel = containerViewModel { AuthViewModel(authRepository, validateCredentials) }`.

**Toàn bộ chuỗi:**
```
MainActivity → VisionFitApp → AppGraph.container (store, MockAuthRepository, use case)
→ phát qua LocalAppContainer → NavHost → AuthRoute → containerViewModel lấy repository, tạo AuthViewModel
→ Submit → authRepository.login → thực chất chạy MockAuthRepository.login → tra store.accounts
```
Test tạo trực tiếp các mảnh, không cần giao diện: `AuthViewModel(app.authRepository, app.validateCredentials)` (`commonTest/.../presentation/FlowTests.kt`, khoảng dòng 168).

**Bài tập đã giao:**
1. Đăng nhập `locked@visionfit.vn` với **sai** mật khẩu thì nhận lỗi gì, so với khi **đúng** mật khẩu, và vì sao?
2. Viết `FakeAuthRepository` mà `login` luôn trả `Failure(NETWORK)`. Tạm đổi 1 dòng trong `AppContainer` để dùng nó, chạy thử nút demo, rồi trả lại như cũ.
3. Ở `identity-service`, khi user bị khóa mà nhập sai mật khẩu thì server trả mã HTTP gì? Server có lộ việc tài khoản bị khóa không?

---

### Bước 5b: Làm rõ DI và CompositionLocal (buổi 2, do user hỏi)

**Cách hiểu ban đầu của user:** tạo trạm phát `LocalAppContainer` chứa các thứ cần DI, bọc ngoài App, khởi tạo trước, rồi trong app chỗ nào cần thì lấy và truyền qua tham số.

User hiểu đúng khoảng 70%. Đã chỉnh 3 chỗ:

1. **`staticCompositionLocalOf { error(...) }` chỉ tạo kênh, chưa phát gì.** Có 4 thao tác:
   - ① tạo kênh: `AppContainer.kt:58`
   - ② tạo nội dung `AppContainer(...)`: `App.kt:66`, dùng `by lazy`
   - ③ phát `LocalAppContainer provides container`: `App.kt:37`
   - ④ bắt sóng `LocalAppContainer.current`: **chỉ duy nhất** ở `ViewModelFactory.kt:18`

   `{ error(...) }` là giá trị mặc định khi không có ai phát, nên quên phát thì crash kèm thông báo rõ ràng.
2. **Container là tham số có giá trị mặc định của `VisionFitApp`.** Test có thể thay toàn bộ bếp. Ví dụ thật ở `jvmTest/.../AppFlowUiTest.kt:37-38`: `VisionFitApp(AppContainer(FixedTimeProvider(now)))`. Khi container được tạo thì mọi `val` bên trong (store, repository, use case) được tạo cùng lúc.
3. **(Quan trọng nhất) Không phải ai cũng bắt sóng.** Có 2 cơ chế khác nhau:
   ```
   App.kt ──(phát sóng)──► Route ──(constructor)──► ViewModel
            = xe chở hàng          = giao hàng tận tay (DI thật sự)
   ```
   - ViewModel **không thể** đọc `LocalXxx.current`, vì nó không phải `@Composable`.
   - ViewModel cũng **không nên** nhận cả container, vì như vậy sẽ giấu đi những gì nó thực sự cần.
   - Screen không biết container tồn tại.

   | Ai | Biết container? | Nhận gì |
   |---|---|---|
   | `App.kt` | Có (tạo và phát) | |
   | `XRoute` | Có (qua `containerViewModel`) | |
   | `XViewModel` | Không | Đúng các repository và use case nó cần |
   | `XScreen` | Không | `state`, `onEvent` |

- **So sánh với Spring:**
  - `AppContainer` tương ứng `ApplicationContext`
  - các `val` tương ứng `@Bean`
  - constructor của ViewModel tương ứng constructor injection
  - Khác biệt: ở đây ta tự viết tay trong `containerViewModel { ... }`, không có framework tự tiêm.

**Bài tập nhỏ (Claude đã chữa):** `private val repo = AppGraph.container.authRepository` bên trong ViewModel có 2 vấn đề:
1. `AppFlowUiTest` truyền vào container dùng bếp giả, nhưng ViewModel lén dùng bếp thật, nên test chập chờn.
2. Constructor rỗng, nên không đưa Fake vào để test riêng được, và phụ thuộc bị giấu đi.

Nguyên tắc rút ra: **class nhận phụ thuộc từ bên ngoài, không tự đi lấy.**

---

### Bước 6.1: `presentation/auth/AuthContract.kt` (hợp đồng giữa Screen và ViewModel)

- **Vấn đề:** nếu dùng nhiều `remember` rời rạc thì sẽ có 2 biến nói cùng một chuyện (`isRegister` và `mode`), lỗi là String nên không biết thuộc ô nào hay thuộc server, và không có danh sách "người dùng làm được gì".
- **Giải pháp:** một file chỉ chứa kiểu dữ liệu (State, Event, Effect), không có logic.
- **Vì sao viết sau bước 5:** file import `AuthError` (Domain) và `CredentialError` (use case). Domain không bao giờ import Contract.
- **Từng khối:**
  - `AuthMode` (dòng 6): một màn hình, hai chế độ.
  - `LegalDocument(url)` (dòng 8): Screen gửi `LegalLinkClicked(TERMS)`, không gửi chuỗi URL. URL chỉ nằm ở một chỗ.
  - `AuthUiState` (dòng 13-28):
    - dữ liệu nhập
    - lỗi từng ô (`CredentialError?`, `null` là không lỗi)
    - lỗi server (`submitError: AuthError?`, hiện banner)
    - 2 cờ loading riêng cho 2 nút
    - mọi field đều có mặc định, nên `AuthUiState()` chính là màn hình lúc mới mở
    - `isRegister` là `get()`, được **tính từ `mode`**, không lưu riêng, nên không bao giờ mâu thuẫn
  - `AuthEvent` (dòng 30-39): `data class` khi có dữ liệu, `data object` khi không. Tên event mô tả việc đã xảy ra (`EmailChanged`), không phải mệnh lệnh. **Không có event `NavigateXxx`.**
  - `AuthEffect` (dòng 41-47): chuyển màn, `OpenUrl` (ViewModel không mở được trình duyệt), `ResetLinkSent`/`ResetLinkFailed` (nếu để trong State thì xoay máy là hiện lại).

**Bài tập đã giao:**
1. Đoán State và Effect cho từng event. Đã có đáp án trong bài 6.2.
2. Thêm ô tick "Ghi nhớ đăng nhập": cần thêm gì vào State, Event, Effect?
3. Nếu `isRegister` là `val ... = false` trong constructor thì có bug gì? Hãy chỉ ra một dòng `copy` cụ thể.

---

### Bước 6.2: `presentation/auth/AuthViewModel.kt`

- **Vấn đề:** hàm `submit` viết ngây thơ (`launch { isSubmitting = true; login(currentState...); if Success navigate }`) có 4 lỗi:
  1. bấm 2 lần thì gọi server 2 lần
  2. không kiểm tra đầu vào
  3. đọc `currentState` **sau khi chờ mạng**, nên đổi chế độ giữa chừng là đi nhầm màn
  4. nhánh `Failure` không làm gì, nút quay mãi
- **Vì sao viết sau Contract:** ViewModel import `MviViewModel` (bước 3), `AuthRepository`/`AuthResult`/`ValidateCredentialsUseCase` (bước 5), và dùng Contract (6.1). Nó là chỗ gặp nhau của mọi thứ đã học.
- **① Constructor** (dòng 10-13): đoạn 2 của DI. `AuthUiState()` là giá trị ban đầu của hộp.
- **② `onEvent`** (dòng 15-36) hoạt động như tổng đài:
  - event đơn giản xử lý inline bằng `updateState`, event phức tạp gọi hàm `private`
  - `sealed` nên quên nhánh nào là compiler báo
  - `ModeSelected`: **giữ email và password**, xóa `confirmPassword` và các lỗi
  - `XChanged`: theo mẫu "cập nhật giá trị, xóa lỗi ô đó, xóa banner"
  - `LegalLinkClicked` chỉ `sendEffect(OpenUrl)`
- **③ `submit()`** (dòng 38-72), đọc theo dòng thời gian:
  1. chụp ảnh `val state = currentState`
  2. `isSubmitting` đang true thì return (sửa lỗi 1)
  3. validate với `confirmation = confirmPassword.takeIf { isRegister }` (sửa lỗi 2)
  4. không hợp lệ thì gán cả 3 lỗi rồi return, không gọi mạng
  5. `isSubmitting = true`, xóa banner
  6. `launch` gọi register hoặc login, **dùng ảnh chụp `state`** (sửa lỗi 3: người vừa đăng ký mà đổi tab giữa chừng vẫn được đưa tới Onboarding)
  7. `Success` thì tắt loading và gửi effect; `Failure` thì tắt loading và gán `submitError` (sửa lỗi 4)
- **④ `requestPasswordReset()`** (dòng 74-89): cùng khuôn với `submit` nhưng nhỏ hơn.
  - chỉ kiểm tra email: `validateCredentials(email, "", null).email`, lỗi mật khẩu sinh ra cũng bị bỏ qua
  - kết quả chỉ là Effect

**Bài tập đã giao:**
1. `ModeSelected` không xóa `emailError`. Đây là cố ý hay sót? Hãy nêu lý lẽ cho cả hai phía.
2. Đang `isSubmitting` mà người dùng bấm "Quên mật khẩu?" thì sao? Có nên chặn không?
3. Viết nhánh `when` cho event "Ghi nhớ đăng nhập". Có cần truyền giá trị này xuống `login()` không? Nếu có thì phải sửa ở những tầng nào?

---

### Bước 6.3: `AuthRoute` (`AuthScreen.kt:87-106`)

- **Vấn đề:** gộp ViewModel và giao diện vào một hàm thì:
  - không `@Preview` được, vì không có `LocalAppContainer` nên crash
  - muốn xem trạng thái "đầy lỗi" thì phải tự gõ
  - code giao diện dính chặt vào ViewModel
- **Giải pháp:**
  - **Route** là phần stateful, đóng vai "dây điện": lấy ViewModel, đọc hộp, nghe ống, dịch effect.
  - **Screen** là phần stateless: chỉ là một hàm `state → giao diện`, preview được với bất kỳ `AuthUiState(...)` nào.
- **Vì sao viết sau ViewModel:** Route dùng `AuthViewModel`. Với Screen, Route chỉ cần biết **chữ ký**, nên có thể viết Route trước rồi viết thân Screen sau.
- **Từng khối:**
  - ① **Tham số:** 2 lambda điều hướng do NavHost truyền vào (`VisionFitNavHost.kt:43-48`), và `viewModel` có giá trị mặc định `containerViewModel { ... }` để test truyền ViewModel khác.
  - ② **Đọc hộp:** `collectAsStateWithLifecycle().value` (dòng 92).
  - ③ **Bắt sóng** `LocalAppMessenger` và `LocalUriHandler` (dòng 93-94). Hai dòng này nằm **bên ngoài** `CollectEffects`. Lý do là bài tập 1 bên dưới: lambda `onEffect` không phải `@Composable`.
  - ④ **`CollectEffects`** (dòng 95-104) dịch effect thành hành động:
    - `Navigate*` gọi lambda tương ứng
    - `OpenUrl` bọc trong `runCatching`, vì không có trình duyệt thì `openUri` ném exception
    - các effect còn lại gọi `messenger.show(...)`
    - **Câu chữ tiếng Việt nằm ở Route**, không nằm ở ViewModel.
  - ⑤ **`AuthScreen(state, viewModel::onEvent)`** (dòng 105): method reference, tương đương `{ e -> viewModel.onEvent(e) }`.
- **Chuỗi 4 trạm với số dòng thật:**
  - ① Screen gọi `onEvent(Submit)`
  - ② `AuthViewModel.kt:67` gửi `sendEffect(NavigateToDashboard)`
  - ③ `AuthScreen.kt:97` gọi `onNavigateToDashboard()`
  - ④ `VisionFitNavHost.kt:45` gọi `navigateToDashboard()`

  `ResetLinkSent` kết thúc luôn ở trạm ③.

**Bài tập đã giao:**
1. Chuyển `LocalUriHandler.current` vào bên trong nhánh `OpenUrl`, build xem lỗi gì và vì sao (xem `CollectEffects.kt:19`).
2. Truy vết nút "Quên mật khẩu?" từ lúc bấm tới khi snackbar hiện, ghi `file:dòng` cho mỗi trạm.
3. Tạo `practice/MyAuthScreen.kt` (thân hàm để trống) và đổi dòng 105 của `AuthRoute` sang gọi nó.

---

## 4. Bài tập chưa nộp hoặc chưa review

1. Theme: dùng token trong `myBlankScreen`, bọc preview, thử đổi `primary`.
2. MVI: bộ đếm có nút −, Reset, effect `ShowMessage`.
3. Navigation: NavHost mini Home/Detail, sau đó thêm `HomeViewModel` để đi đủ 4 trạm.
4. Domain/Data/DI: câu hỏi về tài khoản bị khóa + `FakeAuthRepository` + mã HTTP của `identity-service`.
5. Contract (6.1): "Ghi nhớ đăng nhập" thuộc State/Event/Effect; bug nếu `isRegister` là field lưu riêng.
6. ViewModel (6.2): `emailError` khi `ModeSelected`; bấm "Quên mật khẩu?" lúc đang submit; nhánh `when` cho "Ghi nhớ đăng nhập".
7. Route (6.3): lỗi compile khi đọc `LocalUriHandler` trong lambda effect; truy vết "Quên mật khẩu?"; **tạo `MyAuthScreen` và đổi dòng 105** (cần làm trước 6.4).

Các checkpoint chưa được trả lời:
- "Screen stateless giúp được điều gì?" (6.3)
- 2 câu tóm tắt "CompositionLocal làm gì / constructor làm gì trong DI" (5b)

Khi user nộp bài, hãy review nghiêm: đúng pattern chưa (Screen không biết NavController, State là object bất biến, Effect đi qua Channel…), clean code, edge case.

---

## 5. Kế hoạch các bước tiếp theo (đủ để dạy tiếp)

### Bước 6.4 (TIẾP THEO): User tự viết `MyAuthScreen`

Mở đầu buổi:
1. Hỏi lại các checkpoint còn treo (mục 4).
2. Kiểm tra user đã tạo `practice/MyAuthScreen.kt` và đổi dòng 105 của `AuthRoute` chưa.
3. Dạy theo đúng phong cách: mỗi lượt chỉ một phần nhỏ. Trước khi user dùng khái niệm Compose nào mới (`TextField`/`BasicTextField`, `Column`, `Arrangement`, `KeyboardOptions`…), phải giới thiệu khái niệm đó trước. Đưa snippet nhỏ, user tự gõ, Claude review nghiêm.

**Chia nhỏ việc viết Screen (stateless):**
1. Bản tối giản: ô email, ô mật khẩu, nút Submit, dòng lỗi. Chỉ đọc `state` và gọi `onEvent`.
2. Thêm nút chuyển Đăng nhập/Đăng ký và ô nhập lại mật khẩu (chỉ hiện khi `isRegister`).
3. Thêm banner `submitError` và trạng thái loading của nút (`VfPrimaryButton(loading = state.isSubmitting)`).
4. Thêm hàm dịch enum sang tiếng Việt, đặt ở UI dưới dạng extension private:
   - `CredentialError.message()`: "Nhập email của bạn nhé", "Email chưa đúng định dạng", "Mật khẩu cần tối thiểu 8 ký tự", "Mật khẩu nhập lại chưa khớp", …
   - `AuthError.message()`: "Email hoặc mật khẩu chưa đúng…", "Email này đã có tài khoản rồi…", "Tài khoản đang tạm khóa…", "Không kết nối được…"
5. Thêm `@Preview` bọc bằng `VisionFitPreview`, với state giả (đăng nhập trống, đăng ký đầy lỗi, xoay ngang).

**Tham khảo bản gốc (đã khôi phục đầy đủ, dùng để đối chiếu sau khi user tự viết xong từng phần):**
- `AuthScreen.kt`:
  - chọn layout theo `LocalWindowLayout`: `AuthTwoPanes` (cửa sổ rộng hoặc xoay ngang), `AuthCentered` (tablet dọc), `AuthStacked` (điện thoại, dùng `Layout` tùy biến để phần hero co lại)
  - các mảnh dùng chung `AuthFields`, `AuthActions` (nút + text điều khoản dùng `buildAnnotatedString` với `withLink`), `SubmitErrorBanner`
  - ô nhập dùng `KeyboardOptions`/`KeyboardActions`: Next chuyển sang ô kế, Done thì Submit
  - `AnimatedVisibility` cho ô nhập lại mật khẩu
- `components/AuthForm.kt`:
  - `AuthModeSwitch` (segmented control với `animateColorAsState`)
  - `AuthTextField` (`BasicTextField` + `decorationBox` hiện placeholder; bóng đổi màu khi focus hoặc có lỗi; `semantics { error() }`; nút ẩn/hiện mật khẩu; `FocusRequester`)
- `components/AuthHero.kt`: logo, collage ảnh, tiêu đề. Chỉ để trang trí, không có state.

### Bước 7: Design system components
- `BrutalSurface`:
  - nền phẳng + viền ink 2dp + bóng cứng lệch
  - khi có `onClick` thì thành nút, nhấn vào thì "lún" xuống (`interactionSource`, `collectIsPressedAsState`, `animateFloatAsState` với spring, `graphicsLayer` dịch chuyển)
  - **thứ tự Modifier rất quan trọng**: alpha → dịch chuyển → hardShadow → clickable → clip → background → border. Nối với bài thử padding/background của user.
- `VfPrimaryButton`: xây trên `BrutalSurface`, có `enabled = enabled && !loading`, hiện vòng xoay khi loading, dùng `LocalContentColor`.
- Icon: `VfIcon`, `VfIcons`. Ngoài ra có `Pills`, `Progress`, `TopBars`, `StateViews`.

### Bước 8: Hoàn thiện App shell
- `AppMessenger` + `VfSnackbarHost`: thông báo chạy trong scope của App, nên vẫn hiện sau khi chuyển màn. Phát qua `LocalAppMessenger`. Snackbar đặt ở `Alignment.BottomCenter`, có padding chừa chỗ cho thanh điều hướng.
- `ProvideWindowLayout` / `WindowLayout`:
  - đo bằng `BoxWithConstraints`
  - phân loại chiều rộng COMPACT/MEDIUM/EXPANDED với mốc 600/840dp, chiều cao với mốc 480/900dp
  - các cờ `isNarrow`, `isShort`, `usesNavigationRail`, `usesTwoPanes`, `gutter`
- WindowInsets (`safeDrawing`, `safeArea`, `ime`) để tránh tai thỏ, thanh hệ thống và bàn phím.
- `VisionFitPreview` (theme + WindowLayout).
- `App.kt` bản hoàn chỉnh:
  ```
  Theme → snackbarHostState + messenger → provide container và messenger
  → ProvideWindowLayout → Box(NavHost + SnackbarHost)
  ```

### Bước 9: Test
- `commonTest/presentation/FlowTests.kt`: test ViewModel bằng coroutines-test (`StandardTestDispatcher`, `setMain`/`resetMain`, `advanceTimeBy`, hàm `settle()`), gom effect bằng `collect`.
- `NutritionUseCasesTest`, `VnFormatTest`, `WindowLayoutTest`.
- `jvmTest`: `AppFlowUiTest` (compose ui-test), `ScreenshotSmokeTest`, `ResponsiveScreenshotTest`.

### Bước 10: Nối backend thật
**Code đã có sẵn** (commit `607f553`):
- Ktor trong `libs.versions.toml` và `composeApp/build.gradle.kts`
- `data/remote/`: `ApiConfig` (`expect/actual`, port **8081**), `HttpClientFactory`, `TokenStore`, `dto/AuthDtos`
- `RemoteAuthRepository`: đọc `uid` từ access token, đổi mã HTTP thành `AuthError`
- `AppContainer` chọn bếp bằng `apiBaseUrl`
- Test: `RemoteAuthRepositoryTest`
- Cấu hình cho phép HTTP không mã hóa: Android `network_security_config.xml`, iOS `Info.plist`

Cách dạy: **đọc và review** code có sẵn theo thứ tự phụ thuộc (DTO → client → TokenStore → Repository → AppContainer → test). Với mỗi file, hỏi user "nếu tự viết, bạn sẽ làm khác chỗ nào", và nối với code `identity-service` của user.

---

## 6. Sổ tay: file → vai trò

| File | Vai trò |
|---|---|
| `App.kt` | `VisionFitApp()`, điểm vào chung; `AppGraph` giữ container duy nhất |
| `di/AppContainer.kt` | Bảng phân công interface → bản thực hiện; `LocalAppContainer` |
| `domain/model/*.kt` | Dữ liệu nghiệp vụ (Auth, Meal, Nutrition, BodyProfile, Analysis, DayLog) |
| `domain/repository/Repositories.kt` | Các interface: lời hứa, đồng thời là hợp đồng với backend |
| `domain/usecase/*.kt` | Quy tắc nghiệp vụ thuần (validate, tính TDEE/target, streak, tổng hợp ngày) |
| `data/mock/InMemoryVisionFitStore.kt` | Database giả dùng chung |
| `data/repository/Mock*.kt` | Bản giả thực hiện các interface |
| `core/mvi/MviViewModel.kt` | Lớp cha State/Event/Effect |
| `core/mvi/CollectEffects.kt` | UI nghe effect an toàn theo lifecycle |
| `core/time/TimeProvider.kt` | Đồng hồ thay được, phục vụ test |
| `presentation/designsystem/theme/` | Color, Type, Dimens, Theme |
| `presentation/designsystem/components/` | BrutalSurface, Buttons, … |
| `presentation/designsystem/layout/` | WindowLayout, xử lý responsive |
| `presentation/navigation/` | Routes, VisionFitNavHost |
| `presentation/common/` | `containerViewModel`, `AppMessenger`, các hàm đổi model → chữ hiển thị |
| `presentation/<feature>/` | `XContract.kt`, `XViewModel.kt`, `XScreen.kt` (gồm `XRoute` + `XScreen`), `components/` |

**Công thức thêm một màn hình mới:**
1. Domain: model + interface (+ use case nếu cần)
2. Mock implement interface
3. Đăng ký trong `AppContainer`
4. `XContract.kt`
5. `XViewModel` kế thừa `MviViewModel`, viết test
6. `XScreen` stateless + `@Preview`
7. `XRoute`: tạo ViewModel bằng `containerViewModel`, collect state và effect
8. Thêm route vào `Routes.kt` và `composable<>` vào NavHost
